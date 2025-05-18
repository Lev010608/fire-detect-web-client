# fastapi_service/predictor.py

import cv2
import numpy as np
from PIL import Image
import os
import tempfile

def preprocess_image(image: Image.Image):
    """将 PIL 图像转换为 NumPy 数组"""
    img = np.array(image)
    # 转换为BGR（YOLO通常处理BGR格式的图像）
    img = img[..., ::-1]
    return img


def run_inference(model, image=None, video_path=None):
    """
    使用YOLOv10模型进行推理，处理图片或视频

    参数:
        model: YOLOv10模型实例
        image: PIL Image对象或NumPy数组
        video_path: 视频文件路径

    返回:
        包含检测结果的字典
    """
    detection_results = {}

    try:
        if image is not None:
            # 处理图片
            # 如果是PIL Image，转换为NumPy数组
            if isinstance(image, Image.Image):
                img_np = np.array(image.convert('RGB'))
            else:
                img_np = image

            # 运行模型推理
            results = model(img_np)[0]  # 获取第一个结果

            # 获取检测框信息
            boxes = results.boxes

            # 构建结果字典
            detection_params = []
            if len(boxes) > 0:
                # 获取位置、类别和置信度信息
                location_list = boxes.xyxy.cpu().numpy().tolist() if hasattr(boxes.xyxy, 'cpu') else boxes.xyxy.tolist()
                cls_list = boxes.cls.cpu().numpy().tolist() if hasattr(boxes.cls, 'cpu') else boxes.cls.tolist()
                conf_list = boxes.conf.cpu().numpy().tolist() if hasattr(boxes.conf, 'cpu') else boxes.conf.tolist()

                # 将结果整合到参数列表
                for box, cls, conf in zip(location_list, cls_list, conf_list):
                    detection_params.append({
                        "bbox": [int(x) for x in box],  # x1, y1, x2, y2
                        "class": int(cls),
                        "class_name": model.names[int(cls)],
                        "confidence": float(conf)
                    })

            # 获取标注后的图像
            annotated_image = results.plot()

            # 将结果添加到返回字典
            detection_results['annotated_image'] = Image.fromarray(annotated_image)
            detection_results['params'] = detection_params

        if video_path is not None:
            # 处理视频
            cap = cv2.VideoCapture(video_path)
            if not cap.isOpened():
                raise ValueError(f"Could not open video file: {video_path}")

            # 获取视频属性
            width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
            height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
            fps = cap.get(cv2.CAP_PROP_FPS)

            # 创建临时输出文件
            output_video = tempfile.NamedTemporaryFile(delete=False, suffix='.mp4')
            output_path = output_video.name
            output_video.close()

            # 创建视频写入器
            fourcc = cv2.VideoWriter_fourcc(*'mp4v')
            out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))

            # 初始化结果列表
            all_frame_results = []

            # 处理每一帧
            while cap.isOpened():
                ret, frame = cap.read()
                if not ret:
                    break

                # 运行模型推理
                results = model(frame)[0]

                # 收集此帧的结果
                boxes = results.boxes
                frame_results = []

                if len(boxes) > 0:
                    location_list = boxes.xyxy.cpu().numpy().tolist() if hasattr(boxes.xyxy,
                                                                                 'cpu') else boxes.xyxy.tolist()
                    cls_list = boxes.cls.cpu().numpy().tolist() if hasattr(boxes.cls, 'cpu') else boxes.cls.tolist()
                    conf_list = boxes.conf.cpu().numpy().tolist() if hasattr(boxes.conf, 'cpu') else boxes.conf.tolist()

                    for box, cls, conf in zip(location_list, cls_list, conf_list):
                        frame_results.append({
                            "bbox": [int(x) for x in box],  # x1, y1, x2, y2
                            "class": int(cls),
                            "class_name": model.names[int(cls)],
                            "confidence": float(conf)
                        })

                all_frame_results.append(frame_results)

                # 绘制标注并写入输出视频
                annotated_frame = results.plot()
                out.write(annotated_frame)

            # 释放资源
            cap.release()
            out.release()

            detection_results['annotated_video'] = output_path
            detection_results['params'] = all_frame_results  # 返回每帧的检测结果

    except Exception as e:
        import traceback
        traceback.print_exc()
        detection_results['error'] = str(e)

    return detection_results