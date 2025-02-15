# ONNX Model Runner

This guide explains how to run and inspect the `stemmie-convnet-v1.1.onnx` model using Python.

## Prerequisites

- Python 3.x
- Virtual environment (venv)

## Setup

1. Create a Python virtual environment:

```bash
python3 -m venv .venv
```

2. Activate the virtual environment:

- On Linux/macOS:
```bash
source .venv/bin/activate
```
- On Windows:
```bash
.venv\Scripts\activate
```

3. Install required packages:

```bash
pip install onnxruntime numpy
```

## Files

- `stemmie-convnet-v1.1.onnx`: The ONNX model file
- `run_onnx_model.py`: Python script to inspect and run the model

## Model Information

The model is a neural network with the following specifications:

### Inputs
- Main input (`l_x_`): shape [1, 1024]
- 10 LSTM hidden state inputs: each with shape [2, 1, 512]
  - `l_hidden_states_0_0_` through `l_hidden_states_4_1_`

### Outputs
- Main output (`view_69`): shape [1, 4, 1, 1024]
- 10 LSTM hidden state outputs: each with shape [2, 1, 512]
  - Including post-encode, frequency, and pre-mask states

## Running the Model

1. Make sure your virtual environment is activated (you should see `(.venv)` in your terminal prompt)

2. Run the inspection script:
```bash
python run_onnx_model.py
```

3. When you're done, deactivate the virtual environment:
```bash
deactivate
```

## Troubleshooting

If you see an error like:
```
ModuleNotFoundError: No module named 'onnxruntime'
```
This means you're not running Python from the virtual environment. Make sure to activate it using the commands in the Setup section.

## Notes

- The model appears to be designed for audio processing, possibly for stem separation
- The LSTM hidden states need to be maintained between inference calls if processing sequential data
- Input data should be properly preprocessed to match the expected input shape [1, 1024] 