# fastapi_service/main.py

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from io import BytesIO
from PIL import Image
import cv2
import numpy as np
import os
import tempfile
import uuid
import sys
import logging
import time

# 添加WebSocket支持
from fastapi import WebSocket, WebSocketDisconnect
from typing import Dict, Optional
import uuid
import json
import base64

# 导入视频流处理器
try:
    # 相对导入
    from .stream_processor import VideoStreamProcessor
except ImportError:
    try:
        # 从包导入
        from fastapi_service.stream_processor import VideoStreamProcessor
    except ImportError:
        # 直接导入（与文件同目录）
        from stream_processor import VideoStreamProcessor

# 配置日志
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# 获取当前脚本的绝对路径
current_dir = os.path.dirname(os.path.abspath(__file__))

# 获取项目根目录
project_root = os.path.abspath(os.path.join(current_dir, '..'))

# 将项目根目录添加到Python路径
if project_root not in sys.path:
    sys.path.insert(0, project_root)
    logger.info(f"Added {project_root} to sys.path")

# 现在可以导入Config和其他模块
try:
    import Config

    logger.info("Successfully imported Config in main.py")
except ImportError as e:
    logger.error(f"Failed to import Config in main.py: {e}")
    raise

try:
    from .model_loader import yolo_model
    from .predictor import run_inference
except ImportError:
    # 当直接运行此文件而不是作为模块导入时，使用不同的导入路径
    from model_loader import yolo_model
    from predictor import run_inference

import shutil

app = FastAPI()

# 添加静态文件服务 ================
from fastapi.staticfiles import StaticFiles

# 创建静态文件目录
static_dir = os.path.join(current_dir, "static")
os.makedirs(static_dir, exist_ok=True)

# 挂载静态文件目录
app.mount("/static", StaticFiles(directory=static_dir), name="static")
# =========================================

# 创建临时目录用于存储处理后的图像和视频
UPLOAD_DIR = os.path.join(tempfile.gettempdir(), "yolo_api_uploads")
RESULT_DIR = os.path.join(tempfile.gettempdir(), "yolo_api_results")

# 确保目录存在
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(RESULT_DIR, exist_ok=True)

logger.info(f"Upload directory: {UPLOAD_DIR}")
logger.info(f"Result directory: {RESULT_DIR}")




@app.post("/detect")
async def detect_fire_smoke(file: UploadFile = File(...)):
    """接收上传的图片或视频，并返回YOLOv10目标检测结果"""
    try:
        # 检查模型是否加载成功
        if yolo_model is None:
            raise HTTPException(
                status_code=500,
                detail="Model is not loaded properly. Please check server logs or use the /health endpoint for diagnostics."
            )

        # 创建唯一的文件名
        unique_id = str(uuid.uuid4())

        # 判断上传的文件类型
        content_type = file.content_type
        logger.info(f"Received file: {file.filename}, content type: {content_type}")

        # 处理图片
        if content_type.startswith("image"):
            # 读取上传的图片数据
            contents = await file.read()

            # 保存原始图片
            file_extension = os.path.splitext(file.filename)[1]
            input_path = os.path.join(UPLOAD_DIR, f"{unique_id}{file_extension}")
            with open(input_path, "wb") as f:
                f.write(contents)
            logger.info(f"Saved original image to: {input_path}")

            # 使用与GUI相同的方式读取图片
            img = cv2.imdecode(np.frombuffer(contents, np.uint8), cv2.IMREAD_COLOR)
            logger.info(f"Image shape: {img.shape}, dtype: {img.dtype}")

            # 运行YOLOv10推理
            start_time = time.time()
            results = yolo_model(img)[0]  # 直接使用OpenCV读取的图像进行推理
            end_time = time.time()
            logger.info(f"Inference took {(end_time - start_time) * 1000:.2f} ms")

            # 获取检测框信息
            boxes = results.boxes
            logger.info(f"Found {len(boxes)} boxes")

            # 构建结果字典
            detection_params = []
            if len(boxes) > 0:
                # 获取位置、类别和置信度信息
                location_list = boxes.xyxy.cpu().numpy().tolist() if hasattr(boxes.xyxy, 'cpu') else boxes.xyxy.tolist()
                cls_list = boxes.cls.cpu().numpy().tolist() if hasattr(boxes.cls, 'cpu') else boxes.cls.tolist()
                conf_list = boxes.conf.cpu().numpy().tolist() if hasattr(boxes.conf, 'cpu') else boxes.conf.tolist()

                # 将结果整合到参数列表
                for box, cls, conf in zip(location_list, cls_list, conf_list):
                    cls_int = int(cls)
                    class_name = Config.CH_names[cls_int] if cls_int < len(Config.CH_names) else f"未知类别{cls_int}"
                    logger.info(f"Detected: {class_name} (class {cls_int}) with confidence {conf:.4f} at {box}")
                    detection_params.append({
                        "bbox": [int(x) for x in box],  # x1, y1, x2, y2
                        "class": cls_int,
                        "class_name": class_name,
                        "confidence": float(conf)
                    })

            # 获取标注后的图像
            annotated_image = results.plot()

            # 保存标注后的图片
            output_path = os.path.join(RESULT_DIR, f"{unique_id}_result{file_extension}")
            cv2.imwrite(output_path, annotated_image)  # 使用OpenCV保存
            logger.info(f"Saved annotated image to: {output_path}")

            return {
                "file_type": "image",
                "results": detection_params,
                "annotated_image": output_path,
                "detection_count": len(detection_params),
                "class_names": Config.CH_names,  # 添加类别名称映射
                "inference_time": f"{(end_time - start_time) * 1000:.2f} ms"
            }

        # 处理视频
        elif content_type.startswith("video"):
            # 读取上传的视频数据
            contents = await file.read()

            # 保存原始视频
            file_extension = os.path.splitext(file.filename)[1]
            input_path = os.path.join(UPLOAD_DIR, f"{unique_id}{file_extension}")
            with open(input_path, "wb") as f:
                f.write(contents)
            logger.info(f"Saved original video to: {input_path}")

            # 运行YOLOv10推理
            detection_results = run_inference(yolo_model, video_path=input_path)

            if 'error' in detection_results:
                raise HTTPException(status_code=500, detail=detection_results['error'])

            # 复制标注后的视频到结果目录
            output_path = os.path.join(RESULT_DIR, f"{unique_id}_result{file_extension}")
            shutil.copy2(detection_results['annotated_video'], output_path)
            logger.info(f"Saved annotated video to: {output_path}")

            # 计算检测到的物体总数
            total_detections = sum(len(frame_results) for frame_results in detection_results['params'])

            return {
                "file_type": "video",
                "results": detection_results['params'],  # 每帧的检测结果
                "annotated_video": output_path,
                "detection_count": total_detections,
                "class_names": Config.CH_names  # 添加类别名称映射
            }

        else:
            raise HTTPException(status_code=400, detail="Unsupported file type. Only images and videos are supported.")


    except Exception as e:

        import traceback

        error_traceback = traceback.format_exc()

        logger.error(f"Error in detect_fire_smoke: {str(e)}\n{error_traceback}")

        raise HTTPException(status_code=500, detail=str(e))


@app.get("/result/{filename}")
async def get_result(filename: str):
    """获取处理后的图片或视频文件"""
    file_path = os.path.join(RESULT_DIR, filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(file_path)


@app.get("/health")
async def health_check():
    """健康检查端点"""
    model_info = {
        "loaded": yolo_model is not None,
        "names": getattr(yolo_model, "names", {}),
        "model_path": getattr(yolo_model, "ckpt_path", "unknown")
    }

    # 检查项目中的模型文件
    model_files = []
    runs_dir = os.path.join(project_root, 'runs')
    if os.path.exists(runs_dir):
        for root, dirs, files in os.walk(runs_dir):
            for file in files:
                if file.endswith('.pt'):
                    model_files.append(os.path.join(root, file))

    # 检查临时上传文件夹
    upload_files = []
    if os.path.exists(UPLOAD_DIR):
        upload_files = [os.path.join(UPLOAD_DIR, f) for f in os.listdir(UPLOAD_DIR)]

    # 检查结果文件夹
    result_files = []
    if os.path.exists(RESULT_DIR):
        result_files = [os.path.join(RESULT_DIR, f) for f in os.listdir(RESULT_DIR)]

    return {
        "status": "ok" if yolo_model is not None else "error",
        "model": model_info,
        "config": {
            "model_path": Config.model_path,
            "names": Config.names,
            "ch_names": Config.CH_names
        },
        "available_models": model_files,
        "upload_dir": {
            "path": UPLOAD_DIR,
            "files": upload_files
        },
        "result_dir": {
            "path": RESULT_DIR,
            "files": result_files
        }
    }


# 创建视频流处理器实例
stream_processor = None


# 在应用初始化后初始化视频流处理器
@app.on_event("startup")
async def startup_event():
    global stream_processor
    # 确保yolo_model已加载
    if yolo_model is not None:
        stream_processor = VideoStreamProcessor(yolo_model, Config, logger)
        logger.info("Video stream processor initialized successfully")

        # 检查测试页面是否存在
        test_page_path = os.path.join(static_dir, "video_stream_test.html")
        if os.path.exists(test_page_path):
            logger.info(f"Test page found at: {test_page_path}")
        else:
            logger.warning(f"Test page not found at: {test_page_path}. Please create it manually.")
    else:
        logger.warning("YOLOv10 model not loaded, video streaming will not work properly")


# WebSocket连接端点
@app.websocket("/ws/video_stream")
async def video_stream_endpoint(websocket: WebSocket):
    """
    用于实时视频流分析的WebSocket端点

    客户端可以发送：
    1. 单个图像帧进行分析
    2. 视频文件路径进行批量分析
    3. 控制命令（开始/停止处理）
    """
    client_id = str(uuid.uuid4())
    await websocket.accept()

    if stream_processor is None:
        await websocket.send_json({
            "type": "error",
            "message": "Video stream processor is not initialized"
        })
        await websocket.close()
        return

    try:
        # 注册WebSocket连接
        await stream_processor.register_connection(websocket, client_id)

        # 处理客户端消息
        while True:
            # 接收客户端消息
            data = await websocket.receive_text()
            message = json.loads(data)
            msg_type = message.get("type", "")

            # 根据消息类型处理
            if msg_type == "frame":
                # 处理单个图像帧
                frame_data = message.get("data")
                frame_id = message.get("frame_id")
                await stream_processor.process_frame(frame_data, client_id, frame_id)

            elif msg_type == "video_path":
                # 处理视频文件
                video_path = message.get("path")
                save_output = message.get("save_output", False)
                output_path = message.get("output_path")

                # 验证视频文件是否存在
                if not os.path.exists(video_path):
                    await websocket.send_json({
                        "type": "error",
                        "message": f"Video file not found: {video_path}"
                    })
                    continue

                # 开始处理视频
                await stream_processor.start_video_processing(
                    video_path, client_id, save_output, output_path
                )

            elif msg_type == "stop":
                # 停止处理
                if client_id in stream_processor.stop_signals:
                    stream_processor.stop_signals[client_id].set()
                    await websocket.send_json({
                        "type": "stopped",
                        "message": "Processing stopped by client request"
                    })

            elif msg_type == "ping":
                # 心跳检测
                await websocket.send_json({
                    "type": "pong",
                    "timestamp": time.time()
                })

            else:
                # 未知消息类型
                await websocket.send_json({
                    "type": "error",
                    "message": f"Unknown message type: {msg_type}"
                })

    except WebSocketDisconnect:
        logger.info(f"Client {client_id} disconnected")
    except Exception as e:
        logger.error(f"Error in WebSocket connection: {str(e)}")
        import traceback
        logger.error(traceback.format_exc())
    finally:
        # 清理连接
        stream_processor.unregister_connection(client_id)


# 添加上传视频并通过WebSocket流式处理的REST端点
@app.post("/stream_video")
async def stream_video(file: UploadFile = File(...)):
    """
    上传视频文件并返回WebSocket连接信息，用于实时流式处理
    """
    try:
        # 检查模型是否加载成功
        if yolo_model is None:
            raise HTTPException(
                status_code=500,
                detail="Model is not loaded properly. Please check server logs or use the /health endpoint for diagnostics."
            )

        # 验证文件是视频
        content_type = file.content_type
        if not content_type.startswith("video"):
            raise HTTPException(status_code=400, detail="Uploaded file must be a video")

        # 创建唯一的文件名
        unique_id = str(uuid.uuid4())

        # 保存上传的视频
        file_extension = os.path.splitext(file.filename)[1]
        input_path = os.path.join(UPLOAD_DIR, f"{unique_id}{file_extension}")
        output_path = os.path.join(RESULT_DIR, f"{unique_id}_result{file_extension}")

        # 读取和保存视频数据
        contents = await file.read()
        with open(input_path, "wb") as f:
            f.write(contents)
        logger.info(f"Saved uploaded video to: {input_path}")

        # 生成WebSocket连接信息
        websocket_url = f"ws://localhost:8000/ws/video_stream"
        client_id = str(uuid.uuid4())

        # 返回连接信息和命令
        return {
            "status": "success",
            "message": "Video uploaded successfully, use WebSocket for streaming",
            "video_path": input_path,
            "output_path": output_path,
            "websocket": {
                "url": websocket_url,
                "client_id": client_id,
                "command": {
                    "type": "video_path",
                    "path": input_path,
                    "save_output": True,
                    "output_path": output_path
                }
            }
        }

    except Exception as e:
        import traceback
        error_traceback = traceback.format_exc()
        logger.error(f"Error in stream_video: {str(e)}\n{error_traceback}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/test_video_stream")
async def test_video_stream():
    """返回视频流测试页面"""
    return FileResponse(os.path.join(static_dir, "video_stream_test.html"))


@app.post("/update_class_names")
async def update_class_names():
    """手动更新类别名称映射关系"""
    global yolo_model

    if yolo_model is None:
        return {"status": "error", "message": "Model is not loaded"}

    # 查看当前模型的类别映射
    original_names = getattr(yolo_model, "names", {})

    # 打印当前模型类别映射和Config类别映射
    return {
        "status": "success",
        "message": "Class names checked",
        "model_names": original_names,
        "config_names": Config.names,
        "config_ch_names": Config.CH_names
    }


@app.get("/model_details")
async def model_details():
    """获取模型详细信息"""
    if yolo_model is None:
        return {"status": "error", "message": "Model is not loaded"}

    # 调用模型进行一次测试推理
    test_img = np.zeros((640, 640, 3), dtype=np.uint8)
    test_img[100:200, 100:200, 0] = 255  # 红色方块

    results = yolo_model(test_img)[0]

    return {
        "status": "success",
        "model_info": {
            "path": getattr(yolo_model, "ckpt_path", "unknown"),
            "names": getattr(yolo_model, "names", {}),
            "task": getattr(yolo_model, "task", "unknown"),
        },
        "config_names": Config.names,
        "config_ch_names": Config.CH_names,
        "test_inference": {
            "shape": results.boxes.shape,
            "device": str(results.boxes.device) if hasattr(results.boxes, "device") else "unknown"
        }
    }




# 添加启动服务的代码
def start():
    """启动FastAPI服务器"""
    import uvicorn
    logger.info("Starting FastAPI server...")
    uvicorn.run(app, host="0.0.0.0", port=8000)


if __name__ == "__main__":
    logger.info("=== Starting YOLOv10 Fire and Smoke Detection API ===")
    try:
        # 直接启动服务器，不使用uvicorn.run
        import uvicorn

        uvicorn.run(app, host="0.0.0.0", port=8000)
    except Exception as e:
        logger.error(f"Error starting server: {e}")
        import traceback

        logger.error(traceback.format_exc())