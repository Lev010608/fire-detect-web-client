import os
from ultralytics.models.yolov10 import YOLOv10  # 使用正确的YOLOv10类

# 获取当前脚本的绝对路径
current_dir = os.path.dirname(os.path.abspath(__file__))

# 定义相对路径到模型
model_path = os.path.join(current_dir, '..', 'runs', 'detect', 'exp', 'weights', 'best.pt')

# 加载模型
def load_model(model_path=model_path):
    print(f"Loading YOLOv10 model from {model_path}...")
    try:
        model = YOLOv10(model_path, task='detect')  # 指定task为detect
        # 预热模型
        model(np.zeros((48, 48, 3)))
        print("Model loaded successfully!")
        return model
    except Exception as e:
        print(f"Error loading model: {e}")
        raise

# 在服务启动时加载模型
import numpy as np
yolo_model = load_model()