# fastapi_service/model_loader.py

import os
import sys
import numpy as np

# 获取当前脚本的绝对路径
current_dir = os.path.dirname(os.path.abspath(__file__))

# 获取项目根目录
project_root = os.path.abspath(os.path.join(current_dir, '..'))

# 将项目根目录添加到Python路径
if project_root not in sys.path:
    sys.path.insert(0, project_root)
    print(f"Added {project_root} to sys.path")

# 导入项目配置
try:
    import Config

    print("Successfully imported Config")
except ImportError as e:
    print(f"Failed to import Config: {e}")
    raise

# 导入YOLOv10类
try:
    from ultralytics.models.yolov10.model import YOLOv10

    print(f"Successfully imported YOLOv10 class")
except ImportError as e:
    print(f"Failed to import YOLOv10: {e}")
    raise


# 检查模型文件是否存在
def find_model_path():
    # 尝试Config中的路径
    config_path = os.path.join(project_root, Config.model_path)
    if os.path.exists(config_path):
        print(f"Found model at Config path: {config_path}")
        return config_path

    # 尝试GUI中使用的路径
    gui_path = os.path.join(project_root, 'runs/detect/exp/weights/best.pt')
    if os.path.exists(gui_path):
        print(f"Found model at GUI path: {gui_path}")
        return gui_path

    # 搜索可能的模型路径
    for folder in os.listdir(os.path.join(project_root, 'runs/detect')):
        potential_path = os.path.join(project_root, f'runs/detect/{folder}/weights/best.pt')
        if os.path.exists(potential_path):
            print(f"Found model at path: {potential_path}")
            return potential_path

    # 如果找不到，返回原始路径并打印警告
    print(f"WARNING: Could not find model file. Please check the path in Config.py")
    return os.path.join(project_root, 'runs/detect/exp/weights/best.pt')


# 获取实际可用的模型路径
model_path = find_model_path()
print(f"Using model path: {model_path}")


# 加载模型
def load_model(model_path=model_path):
    print(f"Loading YOLOv10 model from {model_path}...")
    try:
        model = YOLOv10(model_path, task='detect')  # 指定task为detect

        # 不直接修改names属性，而是在后续处理中使用Config中的名称
        print(f"Model loaded with names: {model.names}")
        print(f"We will use Config names: {Config.names} when processing results")

        # 预热模型
        model(np.zeros((48, 48, 3)))  # 预热模型
        print("Model loaded successfully!")
        return model
    except Exception as e:
        print(f"Error loading model: {e}")
        import traceback
        traceback.print_exc()

        # 如果加载失败，尝试寻找默认配置
        try:
            print("Trying fallback to yolov8n.pt...")
            model = YOLOv10("yolov8n.pt", task='detect')
            print(f"Model loaded with names: {model.names}")
            print(f"We will use Config names: {Config.names} when processing results")
            model(np.zeros((48, 48, 3)))
            print("Fallback model loaded successfully!")
            return model
        except Exception as fallback_err:
            print(f"Error loading fallback model: {fallback_err}")
            raise e  # 抛出原始错误


# 在服务启动时加载模型
try:
    yolo_model = load_model()
except Exception as e:
    print(f"Error initializing model. Starting with a None model: {e}")
    yolo_model = None