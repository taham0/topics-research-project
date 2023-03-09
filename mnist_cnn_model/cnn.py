import tensorflow as tf
from tensorflow.python.keras import Model, layers, Sequential
import numpy as np
import warnings


IMAGE_SIZE = 28


class ClientModel(Model):
    def __init__(self, seed, lr, num_classes, types='H', masks=None):
        self.num_classes = num_classes
        self.type = types
        self.masks = masks  # used for "L" type
        super(ClientModel, self).__init__()
        self.optimizer = tf.keras.optimizers.Adam(lr=lr)

        if self.type == 'H':
            # Instantiate Input layers , like placeholders in tf1
            labels = tf.keras.Input(
                shape=(None,), dtype=tf.int64, name='labels')  # none mean dynamic shape depending on number of sample images
            features = tf.keras.Input(
                shape=(None, IMAGE_SIZE * IMAGE_SIZE), name='features')

            # reshape the features to 28x28x1
            # (batch_size, height, width, channels)
            x = tf.reshape(features, [-1, IMAGE_SIZE, IMAGE_SIZE, 1])

            # Instantiate Convolutional layers
            self.layer_stack = Sequential([
                layers.Conv2D(32, (5, 5), padding="same",
                              activation=tf.nn.relu, name="conv1"),
                layers.MaxPooling2D((2, 2), strides=2),
                layers.Conv2D(64, (5, 5), padding="same",
                              activation=tf.nn.relu, name="conv_last"),
                layers.MaxPooling2D((2, 2), strides=2)
            ])
            # changing 2048 to 256 still gives very high accuracy and speeds up training
            self.classifier = Sequential([
                layers.Flatten(),
                layers.Dense(2048, activation=tf.nn.relu, name='dense1'),
                layers.Dense(self.num_classes, name='logits')
            ])

            self.out_layer = layers.Softmax()
        # code for "L" type

    def call(self, features):
        """Model function for CNN."""
        # activate the layers
        if self.type == 'H':
            x = self.layer_stack(features)
            logits = self.classifier(x)

            return self.out_layer(logits)

            # predictions = {
            #     "classes": tf.argmax(input=logits, axis=1),
            #     "probabilities": tf.nn.softmax(logits, name="softmax_tensor")
            # }

            # return predictions


if __name__ == '__main__':
    warnings.filterwarnings('ignore')

    model = ClientModel(1, 0.001, 10, 'H')  # intitialize the model

    # Build the model
    # (batch_size, height, width, channels) , batch size is dynamic
    model.build(input_shape=(None, 28, 28, 1))

    # Print out number of parameters
    # print(f"Number of parameters: {model.count_params()}")
    print(model.summary())

    # Load in MNIST data
    mnist = tf.keras.datasets.mnist
    # download may take a while
    (x_train, y_train), (x_test, y_test) = mnist.load_data()
    x_train, x_test = x_train / 255.0, x_test / 255.0

    # Fix the shape to be 28x28x1
    x_train = np.reshape(x_train, (-1, 28, 28, 1))
    x_test = np.reshape(x_test, (-1, 28, 28, 1))

    # one hot encode the labels
    y_train = tf.keras.utils.to_categorical(y_train, 10)
    y_test = tf.keras.utils.to_categorical(y_test, 10)

    # features = tf.random.normal([32, 28, 28, 1])
    # labels = tf.random.normal([32, 10])

    # Compile the model
    model.compile(optimizer='adam',
                  loss='categorical_crossentropy',
                  metrics=['accuracy'])

    print(f"-----------------------------COMPILED THE MODEL-----------------------------")

    # Train the model
    # model.fit(features, labels, epochs=5)
    model.fit(x_train, y_train, epochs=5)
