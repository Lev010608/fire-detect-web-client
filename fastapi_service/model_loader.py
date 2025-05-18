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

# 输出当前 Python 路径以进行调试
print(f"Python path: {sys.path}")

# 检查是否能找到 ultralytics 模块
try:
    import ultralytics
    print(f"Found ultralytics module at: {ultralytics.__file__}")
except ImportError as e:
    print(f"Failed to import ultralytics: {e}")
    print("Available modules in project root:", os.listdir(project_root))
    raise

# 尝试直接从模型文件导入
try:
    from ultralytics.models.yolov10.model import YOLOv10
    print(f"Successfully imported YOLOv10 class")
except ImportError as e:
    print(f"Failed to import YOLOv10: {e}")
    # 检查文件是否存在
    yolov10_path = os.path.join(project_root, 'ultralytics', 'models', 'yolov10')
    print(f"Contents of {yolov10_path}:", os.listdir(yolov10_path) if os.path.exists(yolov10_path) else "Directory not found")
    raise

# 定义相对路径到模型
model_path = os.path.join(current_dir, '..', 'runs', 'detect', 'exp', 'weights', 'best.pt')

# 加载模型
def load_model(model_path=model_path):
    print(f"Loading YOLOv10 model from {model_path}...")
    try:
        model = YOLOv10(model_path, task='detect')  # 指定task为detect
        # 预热模型
        model(np.zeros((48, 48, 3)))  # 预热模型
        print("Model loaded successfully!")
        return model
    except Exception as e:
        print(f"Error loading model: {e}")
        import traceback
        traceback.print_exc()
        raise

# 在服务启动时加载模型
yolo_model = load_model()