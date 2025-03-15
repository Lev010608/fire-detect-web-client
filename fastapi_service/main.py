from fastapi import FastAPI, UploadFile, File
from io import BytesIO
from PIL import Image
from model_loader import yolo_model
from predictor import run_inference  # 引入run_inference函数
import os
from tempfile import NamedTemporaryFile
import shutil

app = FastAPI()

@app.post("/detect")
async def detect_fire_smoke(file: UploadFile = File(...)):
    """接收上传的图片或视频，并返回 YOLOv10 目标检测结果"""
    try:
        # 判断上传的文件类型
        content_type = file.content_type

        # 读取文件并转换为 PIL Image 或视频
        if content_type.startswith("image"):
            # 处理图片
            image_data = await file.read()
            image = Image.open(BytesIO(image_data))

            # 运行YOLOv10推理
            detection_results = run_inference(yolo_model, image=image)

            # 保存标注后的图片
            annotated_image = detection_results['annotated_image']
            annotated_image_path = save_image(annotated_image)

            return {"results": detection_results['params'], "image": annotated_image_path}

        elif content_type.startswith("video"):
            # 处理视频
            video_data = await file.read()
            temp_video_path = save_video(video_data)

            # 运行YOLOv10推理
            detection_results = run_inference(yolo_model, video_path=temp_video_path)

            # 保存标注后的视频
            annotated_video_path = detection_results['annotated_video']

            return {"results": detection_results['params'], "video": annotated_video_path}

        else:
            return {"error": "Unsupported file type"}

    except Exception as e:
        return {"error": str(e)}

# 辅助函数：保存图片
def save_image(image):
    temp_file = NamedTemporaryFile(delete=False, suffix='.jpg')
    image.save(temp_file)
    return temp_file.name

# 辅助函数：保存视频
def save_video(video_data):
    temp_file = NamedTemporaryFile(delete=False, suffix='.mp4')
    with open(temp_file.name, 'wb') as f:
        f.write(video_data)
    return temp_file.name

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
