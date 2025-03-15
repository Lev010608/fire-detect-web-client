import torch
import cv2
from io import BytesIO

import numpy as np
from PIL import Image
from ultralytics.models.yolov10 import YOLOv10DetectionPredictor


def preprocess_image(image: Image.Image):
    """将 PIL 图像转换为 NumPy 数组"""
    img = np.array(image)
    return img


def run_inference(model, image=None, video_path=None):
    detection_results = {}

    if image:
        # 处理图片的推理
        results = model(image)
        annotated_image = results.plot()  # 标注图像
        detection_params = results.pandas().xywh[0].to_dict(orient="records")  # 获取检测框参数
        detection_results['annotated_image'] = Image.fromarray(annotated_image)  # 转回为PIL图像
        detection_results['params'] = detection_params

    if video_path:
        # 处理视频的推理
        cap = cv2.VideoCapture(video_path)
        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        out_path = "annotated_video.mp4"
        out = cv2.VideoWriter(out_path, fourcc, 30.0, (640, 480))

        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break

            # 执行推理
            results = model(frame)
            annotated_frame = results.plot()
            out.write(annotated_frame)

        cap.release()
        out.release()
        detection_results['annotated_video'] = out_path
        detection_results['params'] = results.pandas().xywh[0].to_dict(orient="records")

    return detection_results
