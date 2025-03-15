import torch
import cv2
from io import BytesIO

import numpy as np
from PIL import Image
from ultralytics.models.yolov10 import YOLOv10
from ultralytics.models.yolov10 import YOLOv10DetectionPredictor


def preprocess_image(image: Image.Image):
    """将 PIL 图像转换为 NumPy 数组"""
    img = np.array(image)
    return img


def run_inference(model: YOLOv10, image: Image = None, video_path: str = None):
    detection_results = {}

    if image:
        # 将图片转换为 NumPy 数组，并传递给模型进行推理
        img_np = np.array(image)
        predictor = YOLOv10DetectionPredictor(model=model)
        results = predictor.postprocess(predictor.predict(img_np), img_np, [img_np])

        annotated_image = results[0].plot()  # 标注后的图像
        detection_params = results[0].boxes.data.tolist()  # 获取检测框参数
        detection_results['annotated_image'] = Image.fromarray(annotated_image)
        detection_results['params'] = detection_params

    if video_path:
        # 处理视频
        cap = cv2.VideoCapture(video_path)
        fourcc = cv2.VideoWriter_fourcc(*"mp4v")
        out_path = "annotated_video.mp4"
        out = cv2.VideoWriter(out_path, fourcc, 30.0, (640, 480))

        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break

            # 执行推理
            results = predictor.predict(frame)
            annotated_frame = results[0].plot()  # 标注后的视频帧
            out.write(annotated_frame)

        cap.release()
        out.release()
        detection_results['annotated_video'] = out_path
        detection_results['params'] = results[0].boxes.data.tolist()

    return detection_results
