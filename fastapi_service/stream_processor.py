# fastapi_service/stream_processor.py

import asyncio
import base64
import cv2
import json
import numpy as np
import os
import time
import threading
import uuid
from typing import Optional, Dict
from fastapi import WebSocketDisconnect


class VideoStreamProcessor:
    """
    用于处理WebSocket视频流的类
    实现逐帧处理和实时返回结果
    """

    def __init__(self, model, config, logger):
        self.model = model
        self.config = config
        self.logger = logger
        self.active_connections = {}  # 存储活跃的WebSocket连接
        self.processing_threads = {}  # 存储处理线程
        self.stop_signals = {}  # 存储停止信号

    async def register_connection(self, websocket, client_id: str):
        """注册新的WebSocket连接"""
        self.active_connections[client_id] = websocket
        self.stop_signals[client_id] = threading.Event()
        self.logger.info(f"Registered new connection: {client_id}")

        # 发送初始确认消息
        await websocket.send_json({
            "type": "connection_established",
            "client_id": client_id,
            "message": "Connection established with video stream processor"
        })

    def unregister_connection(self, client_id: str):
        """注销WebSocket连接"""
        if client_id in self.active_connections:
            del self.active_connections[client_id]

        # 设置停止信号，中断任何正在进行的处理
        if client_id in self.stop_signals:
            self.stop_signals[client_id].set()
            del self.stop_signals[client_id]

        # 清理任何活跃的线程
        if client_id in self.processing_threads:
            del self.processing_threads[client_id]

        self.logger.info(f"Unregistered connection: {client_id}")

    async def process_frame(self, frame_data, client_id: str, frame_id: Optional[int] = None):
        """处理单个视频帧并返回结果"""
        try:
            # 将base64编码的图像转换为numpy数组
            decoded_data = base64.b64decode(frame_data)
            np_arr = np.frombuffer(decoded_data, np.uint8)
            frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

            # 记录帧大小
            self.logger.info(f"Processing frame {frame_id} for client {client_id}, shape: {frame.shape}")

            # 运行模型推理
            start_time = time.time()
            results = self.model(frame)[0]
            inference_time = time.time() - start_time

            # 获取检测结果
            boxes = results.boxes
            detection_params = []

            if len(boxes) > 0:
                location_list = boxes.xyxy.cpu().numpy().tolist() if hasattr(boxes.xyxy, 'cpu') else boxes.xyxy.tolist()
                cls_list = boxes.cls.cpu().numpy().tolist() if hasattr(boxes.cls, 'cpu') else boxes.cls.tolist()
                conf_list = boxes.conf.cpu().numpy().tolist() if hasattr(boxes.conf, 'cpu') else boxes.conf.tolist()

                for box, cls, conf in zip(location_list, cls_list, conf_list):
                    cls_int = int(cls)
                    class_name = self.config.CH_names[cls_int] if cls_int < len(
                        self.config.CH_names) else f"未知类别{cls_int}"
                    detection_params.append({
                        "bbox": [int(x) for x in box],  # x1, y1, x2, y2
                        "class": cls_int,
                        "class_name": class_name,
                        "confidence": float(conf)
                    })

            # 获取标注后的图像
            annotated_frame = results.plot()

            # 将图像编码为JPEG，然后转换为base64
            _, buffer = cv2.imencode('.jpg', annotated_frame)
            encoded_frame = base64.b64encode(buffer).decode('utf-8')

            # 构建并返回结果
            result = {
                "type": "frame_result",
                "frame_id": frame_id,
                "detections": detection_params,
                "detection_count": len(detection_params),
                "annotated_frame": encoded_frame,
                "inference_time": round(inference_time * 1000, 2)  # 毫秒
            }

            # 发送结果给客户端
            if client_id in self.active_connections:
                await self.active_connections[client_id].send_json(result)
                return result

        except Exception as e:
            self.logger.error(f"Error processing frame {frame_id} for client {client_id}: {str(e)}")
            if client_id in self.active_connections:
                await self.active_connections[client_id].send_json({
                    "type": "error",
                    "frame_id": frame_id,
                    "message": str(e)
                })
            return None

    async def start_video_processing(self, video_path, client_id, save_output=False, output_path=None):
        """处理视频文件，逐帧发送结果"""
        if client_id not in self.active_connections:
            self.logger.error(f"No active connection for client {client_id}")
            return

        websocket = self.active_connections[client_id]
        stop_signal = self.stop_signals.get(client_id)

        # 使用线程处理视频，避免阻塞WebSocket
        processing_thread = threading.Thread(
            target=self._process_video_in_thread,
            args=(video_path, client_id, websocket, stop_signal, save_output, output_path)
        )
        self.processing_threads[client_id] = processing_thread
        processing_thread.start()

        # 发送确认消息
        await websocket.send_json({
            "type": "processing_started",
            "client_id": client_id,
            "video_path": video_path,
            "save_output": save_output,
            "output_path": output_path
        })

    def _process_video_in_thread(self, video_path, client_id, websocket, stop_signal, save_output, output_path):
        """在线程中处理视频"""
        try:
            cap = cv2.VideoCapture(video_path)
            if not cap.isOpened():
                asyncio.run(self._send_error(websocket, f"Could not open video file: {video_path}"))
                return

            # 获取视频属性
            frame_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
            frame_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
            fps = cap.get(cv2.CAP_PROP_FPS)
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

            # 设置视频写入器（如果需要保存）
            video_writer = None
            if save_output and output_path:
                os.makedirs(os.path.dirname(output_path), exist_ok=True)
                fourcc = cv2.VideoWriter_fourcc(*'mp4v')
                video_writer = cv2.VideoWriter(output_path, fourcc, fps, (frame_width, frame_height))

            # 发送视频信息
            asyncio.run(websocket.send_json({
                "type": "video_info",
                "width": frame_width,
                "height": frame_height,
                "fps": fps,
                "total_frames": total_frames
            }))

            # 🔥 新增：统计信息
            total_detections = 0
            all_frame_results = []  # 存储所有帧的检测结果
            processing_start_time = time.time()

            # 处理每一帧
            frame_id = 0
            while True:
                # 检查是否有停止信号
                if stop_signal and stop_signal.is_set():
                    self.logger.info(f"Stopping video processing for client {client_id} due to stop signal")
                    break

                ret, frame = cap.read()
                if not ret:
                    break

                # 运行模型推理
                start_time = time.time()
                results = self.model(frame)[0]
                inference_time = time.time() - start_time

                # 获取检测结果
                boxes = results.boxes
                detection_params = []

                if len(boxes) > 0:
                    location_list = boxes.xyxy.cpu().numpy().tolist() if hasattr(boxes.xyxy,
                                                                                 'cpu') else boxes.xyxy.tolist()
                    cls_list = boxes.cls.cpu().numpy().tolist() if hasattr(boxes.cls, 'cpu') else boxes.cls.tolist()
                    conf_list = boxes.conf.cpu().numpy().tolist() if hasattr(boxes.conf, 'cpu') else boxes.conf.tolist()

                    for box, cls, conf in zip(location_list, cls_list, conf_list):
                        cls_int = int(cls)
                        class_name = self.config.CH_names[cls_int] if cls_int < len(
                            self.config.CH_names) else f"未知类别{cls_int}"
                        detection_params.append({
                            "bbox": [int(x) for x in box],
                            "class": cls_int,
                            "class_name": class_name,
                            "confidence": float(conf)
                        })

                # 🔥 累计检测统计
                total_detections += len(detection_params)
                all_frame_results.append(detection_params)

                # 获取标注后的图像
                annotated_frame = results.plot()

                # 保存到输出视频（如果需要）
                if video_writer:
                    video_writer.write(annotated_frame)

                # 将图像编码为JPEG，然后转换为base64
                _, buffer = cv2.imencode('.jpg', annotated_frame)
                encoded_frame = base64.b64encode(buffer).decode('utf-8')

                # 构建结果
                result = {
                    "type": "frame_result",
                    "frame_id": frame_id,
                    "detections": detection_params,
                    "detection_count": len(detection_params),
                    "annotated_frame": encoded_frame,
                    "inference_time": round(inference_time * 1000, 2),  # 毫秒
                    "progress": {
                        "current": frame_id + 1,
                        "total": total_frames,
                        "percent": round((frame_id + 1) / total_frames * 100, 2)
                    }
                }

                # 发送结果
                asyncio.run(websocket.send_json(result))

                frame_id += 1

                # 控制发送速率，避免客户端过载
                time.sleep(1 / fps)  # 尝试以原视频帧率发送

            # 完成处理，释放资源
            cap.release()
            if video_writer:
                video_writer.release()

            # 🔥 计算处理统计信息
            processing_end_time = time.time()
            total_processing_time = processing_end_time - processing_start_time

            # 🔥 发送完成消息 - 包含完整信息
            complete_message = {
                "type": "processing_complete",
                "client_id": client_id,
                "video_info": {
                    "width": frame_width,
                    "height": frame_height,
                    "fps": fps,
                    "duration": total_frames / fps if fps > 0 else 0
                },
                "processing_stats": {
                    "frames_processed": frame_id,
                    "total_frames": total_frames,
                    "total_detections": total_detections,
                    "processing_time_seconds": round(total_processing_time, 2),
                    "processing_time_ms": round(total_processing_time * 1000, 2),
                    "avg_inference_time": round((total_processing_time / frame_id * 1000), 2) if frame_id > 0 else 0
                },
                "output_info": {
                    "output_path": output_path if save_output else None,
                    "file_size": os.path.getsize(output_path) if (
                                save_output and output_path and os.path.exists(output_path)) else 0
                },
                "detection_results": all_frame_results  # 所有帧的检测结果
            }

            asyncio.run(websocket.send_json(complete_message))

        except Exception as e:
            self.logger.error(f"Error in video processing thread for client {client_id}: {str(e)}")
            asyncio.run(self._send_error(websocket, str(e)))
        finally:
            # 清理资源
            if client_id in self.processing_threads:
                del self.processing_threads[client_id]

    async def process_camera_frame(self, frame_data, client_id: str, frame_id: Optional[int] = None):
        """处理摄像头帧并返回结果"""
        try:
            # 解码base64图像数据
            if ',' in frame_data:
                frame_data = frame_data.split(',')[1]  # 移除data:image/jpeg;base64,前缀

            decoded_data = base64.b64decode(frame_data)
            np_arr = np.frombuffer(decoded_data, np.uint8)
            frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

            if frame is None:
                raise ValueError("Failed to decode camera frame")

            # 记录帧大小
            self.logger.info(f"Processing camera frame {frame_id} for client {client_id}, shape: {frame.shape}")

            # 运行模型推理
            start_time = time.time()
            results = self.model(frame)[0]
            inference_time = time.time() - start_time

            # 获取检测结果
            boxes = results.boxes
            detection_params = []

            if len(boxes) > 0:
                location_list = boxes.xyxy.cpu().numpy().tolist() if hasattr(boxes.xyxy, 'cpu') else boxes.xyxy.tolist()
                cls_list = boxes.cls.cpu().numpy().tolist() if hasattr(boxes.cls, 'cpu') else boxes.cls.tolist()
                conf_list = boxes.conf.cpu().numpy().tolist() if hasattr(boxes.conf, 'cpu') else boxes.conf.tolist()

                for box, cls, conf in zip(location_list, cls_list, conf_list):
                    cls_int = int(cls)
                    class_name = self.config.CH_names[cls_int] if cls_int < len(
                        self.config.CH_names) else f"未知类别{cls_int}"
                    detection_params.append({
                        "bbox": [int(x) for x in box],  # x1, y1, x2, y2
                        "class": cls_int,
                        "class_name": class_name,
                        "confidence": float(conf)
                    })

            # 获取标注后的图像
            annotated_frame = results.plot()

            # 将图像编码为JPEG，然后转换为base64
            _, buffer = cv2.imencode('.jpg', annotated_frame)
            encoded_frame = base64.b64encode(buffer).decode('utf-8')

            # 构建并返回结果
            result = {
                "type": "camera_frame_result",
                "frame_id": frame_id,
                "detections": detection_params,
                "detection_count": len(detection_params),
                "annotated_frame": f"data:image/jpeg;base64,{encoded_frame}",
                "inference_time": round(inference_time * 1000, 2)  # 毫秒
            }

            # 发送结果给客户端
            if client_id in self.active_connections:
                await self.active_connections[client_id].send_json(result)
                return result

        except Exception as e:
            self.logger.error(f"Error processing camera frame {frame_id} for client {client_id}: {str(e)}")
            if client_id in self.active_connections:
                await self.active_connections[client_id].send_json({
                    "type": "error",
                    "frame_id": frame_id,
                    "message": str(e)
                })
            return None

    async def start_camera_stream(self, client_id: str):
        """开始摄像头流处理"""
        if client_id not in self.active_connections:
            self.logger.error(f"No active connection for client {client_id}")
            return

        websocket = self.active_connections[client_id]

        # 发送确认消息
        await websocket.send_json({
            "type": "camera_stream_started",
            "client_id": client_id,
            "message": "Camera stream processing started"
        })

    async def stop_camera_stream(self, client_id: str):
        """停止摄像头流处理"""
        if client_id not in self.active_connections:
            return

        websocket = self.active_connections[client_id]

        # 发送停止确认消息
        await websocket.send_json({
            "type": "camera_stream_stopped",
            "client_id": client_id,
            "message": "Camera stream processing stopped"
        })

    async def _send_error(self, websocket, message):
        """发送错误消息给客户端"""
        try:
            await websocket.send_json({
                "type": "error",
                "message": message
            })
        except Exception as e:
            self.logger.error(f"Error sending error message: {str(e)}")