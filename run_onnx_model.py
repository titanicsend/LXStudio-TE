import onnxruntime as ort
import numpy as np

def run_onnx_model(model_path):
    # Load the ONNX model
    print(f"Loading model from {model_path}")
    session = ort.InferenceSession(model_path, providers=['CPUExecutionProvider'])
    
    # Get model inputs
    input_names = [input.name for input in session.get_inputs()]
    input_shapes = [input.shape for input in session.get_inputs()]
    
    print("\nModel Input Information:")
    for name, shape in zip(input_names, input_shapes):
        print(f"Input '{name}' with shape {shape}")
    
    # Get model outputs
    output_names = [output.name for output in session.get_outputs()]
    output_shapes = [output.shape for output in session.get_outputs()]
    
    print("\nModel Output Information:")
    for name, shape in zip(output_names, output_shapes):
        print(f"Output '{name}' with shape {shape}")

if __name__ == "__main__":
    model_path = "stemmie-convnet-v1.1.onnx"
    run_onnx_model(model_path) 