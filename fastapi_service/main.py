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
from model_loader import yolo_model
from predictor import run_inference
import shutil

app = FastAPI()

# 创建临时目录用于存储处理后的图像和视频
UPLOAD_DIR = os.path.join(tempfile.gettempdir(), "yolo_api_uploads")
RESULT_DIR = os.path.join(tempfile.gettempdir(), "yolo_api_results")

# 确保目录存在
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(RESULT_DIR, exist_ok=True)


@app.post("/detect")
async def detect_fire_smoke(file: UploadFile = File(...)):
    """接收上传的图片或视频，并返回YOLOv10目标检测结果"""
    try:
        # 创建唯一的文件名
        unique_id = str(uuid.uuid4())

        # 判断上传的文件类型
        content_type = file.content_type

        # 处理图片
        if content_type.startswith("image"):
            # 读取上传的图片数据
            contents = await file.read()

            # 保存原始图片
            file_extension = os.path.splitext(file.filename)[1]
            input_path = os.path.join(UPLOAD_DIR, f"{unique_id}{file_extension}")
            with open(input_path, "wb") as f:
                f.write(contents)

            # 打开图片
            image = Image.open(BytesIO(contents))

            # 运行YOLOv10推理
            detection_results = run_inference(yolo_model, image=image)

            if 'error' in detection_results:
                raise HTTPException(status_code=500, detail=detection_results['error'])

            # 保存标注后的图片
            output_path = os.path.join(RESULT_DIR, f"{unique_id}_result{file_extension}")
            detection_results['annotated_image'].save(output_path)

            return {
                "file_type": "image",
                "results": detection_results['params'],
                "annotated_image": output_path,
                "detection_count": len(detection_results['params'])
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

            # 运行YOLOv10推理
            detection_results = run_inference(yolo_model, video_path=input_path)

            if 'error' in detection_results:
                raise HTTPException(status_code=500, detail=detection_results['error'])

            # 复制标注后的视频到结果目录
            output_path = os.path.join(RESULT_DIR, f"{unique_id}_result{file_extension}")
            shutil.copy2(detection_results['annotated_video'], output_path)

            # 计算检测到的物体总数
            total_detections = sum(len(frame_results) for frame_results in detection_results['params'])

            return {
                "file_type": "video",
                "results": detection_results['params'],  # 每帧的检测结果
                "annotated_video": output_path,
                "detection_count": total_detections
            }

        else:
            raise HTTPException(status_code=400, detail="Unsupported file type. Only images and videos are supported.")

    except Exception as e:
        import traceback
        traceback.print_exc()
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
    return {"status": "ok", "model_loaded": yolo_model is not None}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)