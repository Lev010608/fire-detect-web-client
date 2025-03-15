import os
from ultralytics.models.yolov10 import YOLOv10

# 获取当前脚本的绝对路径
current_dir = os.path.dirname(os.path.abspath(__file__))

# 定义相对路径到模型
model_path = os.path.join(current_dir, '..', 'runs', 'detect', 'exp', 'weights', 'best.pt')

# 加载模型
def load_model(model_path=model_path):
    print(f"Loading YOLOv10 model from {model_path}...")
    model = YOLOv10(model=model_path)  # 加载模型
    return model

# 在服务启动时加载模型
yolo_model = load_model()  # 这里会自动加载模型
