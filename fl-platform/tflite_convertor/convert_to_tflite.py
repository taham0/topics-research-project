import tensorflow as tf
from tfltransfer import bases
from tfltransfer import heads
from tfltransfer import optimizers
from tfltransfer.tflite_transfer_converter import TFLiteTransferConverter

"""Define the base model.

To be compatible with TFLite Model Personalization, we need to define a
base model and a head model. The base model is used to extract useful features
from the input data which are then passed to the head model.

Here we are using an identity layer for base model, which just passes the 
input as it is to the head model.
"""

IMAGE_SIZE = 28
NUM_CLASSES = 10

base = tf.keras.Sequential(
    [tf.keras.Input(shape=(IMAGE_SIZE, IMAGE_SIZE, 1)), tf.keras.layers.Lambda(lambda x: x)]
)

base.compile(loss="categorical_crossentropy", optimizer="sgd")
base.save("identity_model", save_format="tf")

"""Define the head model.

This is the model architecture that we will train using Flower. 
"""

head = tf.keras.Sequential(
    [
        tf.keras.Input(shape=(IMAGE_SIZE, IMAGE_SIZE, 1), name='features'),
        tf.keras.layers.Conv2D(16, kernel_size=(5, 5), activation='relu', padding='same', name='conv1'),
        tf.keras.layers.MaxPooling2D(pool_size=(2, 2), strides=2),
        tf.keras.layers.Conv2D(20, kernel_size=(5, 5), activation='relu', padding='same', name='conv_last'),
        tf.keras.layers.MaxPooling2D(pool_size=(2, 2), strides=2),
        tf.keras.layers.Flatten(),
        tf.keras.layers.Dense(1000, activation='relu', name='dense1'),
        tf.keras.layers.Dense(NUM_CLASSES, name='logits'),
    ]
)

head.compile(loss="sparse_categorical_crossentropy", optimizer="sgd", metrics=['accuracy'])


"""Convert the model for TFLite.

Using 10 classes in CIFAR10, learning rate = 1e-3 and batch size = 32

This will generate a directory called tflite_model with five tflite models.
Copy them in your Android code under the assets/model directory.
"""

base_path = bases.saved_model_base.SavedModelBase("identity_model")
converter = TFLiteTransferConverter(
    10, base_path, heads.KerasModelHead(head), optimizers.SGD(1e-3), train_batch_size=32
)

converter.convert_and_save("tflite_model")