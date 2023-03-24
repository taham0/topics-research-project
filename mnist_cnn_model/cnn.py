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
                layers.Dense(256, activation=tf.nn.relu, name='dense1', kernel_regularizer=tf.keras.regularizers.l2(
                    0.001), bias_regularizer=tf.keras.regularizers.l2(0.001)),
                layers.Dense(self.num_classes, name='logits')
            ])

            self.out_stack = Sequential([
                layers.Softmax(name='softmax_tensor')
            ])

            # self.out_layer = layers.Softmax(name='softmax_tensor')
        # code for "L" type
        elif self.type == 'L':
            labels = tf.keras.Input(
                shape=(None,), dtype=tf.int64, name='labels')  # none mean dynamic shape depending on number of sample images
            features = tf.keras.Input(
                shape=(None, IMAGE_SIZE * IMAGE_SIZE), name='features')

            mask_conv1 = self.masks['conv1'][2]
            mask_conv_last = self.masks['conv_last'][2]
            mask_dense1 = self.masks['dense1'][2]

            # filters
            conv1_filters = np.count_nonzero(mask_conv1)
            conv_last_filters = np.count_nonzero(mask_conv_last)
            dense1_filters = np.count_nonzero(mask_dense1)

            self.layer_stack = Sequential([
                layers.Conv2D(filters=conv1_filters, kernel_size=(5, 5), padding="same",
                              activation=tf.nn.relu, name="conv1"),
                layers.MaxPooling2D((2, 2), strides=2),
                layers.Conv2D(filters=conv_last_filters, kernel_size=(5, 5), padding="same",
                              activation=tf.nn.relu, name="conv_last"),
                layers.MaxPooling2D((2, 2), strides=2)
            ])

            self.classifier = Sequential([
                layers.Flatten(),
                layers.Dense(units=dense1_filters,
                             activation=tf.nn.relu, name='dense1', kernel_regularizer=tf.keras.regularizers.l2(0.001), bias_regularizer=tf.keras.regularizers.l2(0.001)),
                layers.Dense(self.num_classes, name='logits')
            ])

            self.out_stack = Sequential([
                layers.Softmax(name='softmax_tensor')
            ])
            # self.out_layer = layers.Softmax(name='softmax_tensor')

    def call(self, features):
        """Model function for CNN."""
        # activate the layers
        if self.type == 'H':
            x = tf.reshape(features, [-1, IMAGE_SIZE, IMAGE_SIZE, 1])
            x = self.layer_stack(features)
            logits = self.classifier(x)
            return self.out_stack(logits)
            # return self.out_layer(logits)
        elif self.type == 'L':
            x = tf.reshape(features, [-1, IMAGE_SIZE, IMAGE_SIZE, 1])
            x = self.layer_stack(features)
            logits = self.classifier(x)
            return self.out_stack(logits)
            # return self.out_layer(logits)
            # predictions = {
            #     "classes": tf.argmax(input=logits, axis=1),
            #     "probabilities": tf.nn.softmax(logits, name="softmax_tensor")
            # }

            # return predictions


def get_dropout_layer_info(model, layer_names):
    """Get the dropout layer information from the model."""
    i = 0
    dropout_layer_info = {}
    for layerstack in model.layers:
        for layer in layerstack.layers:
            # print(layer.name)
            if layer.name in layer_names:
                # print("hell")
                temp = []
                temp.append(i)
                if 'conv' in layer.name:
                    temp.append(int(layer.output_shape[3]))
                elif 'rnn' in layer.name:
                    temp.append(
                        (int(layer.output_shape[0]), int(layer.output_shape[1])))
                else:
                    temp.append(int(layer.output_shape[1]))
                dropout_layer_info[layer.name] = temp
            i += 1

    return dropout_layer_info


def create_masks(model, layer_names, p):
    info = get_dropout_layer_info(model, layer_names)
    for i in info:
        # print(i)
        if "rnn" in i:
            N = info[i][1][1]
            m = np.ones(N, dtype=bool)
            tot = int(N/4)
            dropN = int(N/4 * p)
            dropInd = np.random.choice(tot, dropN, replace=False)
            masks = []
            if "cell_0" in i:
                emb = info[i][1][0]
                emb_m = np.ones(emb, dtype=bool)
                embdropInd = np.random.choice(emb, dropN, replace=False)
                emb_m[embdropInd] = 0
                masks.append(emb_m)

            if "cell_1" in i:  # last rnn layer rnn->dense
                N = tot
                m2 = np.ones(N, dtype=bool)
            else:
                N = tot * 2
                m2 = np.ones(N, dtype=bool)

            for di in dropInd:
                d = di * 4
                np.put(m, [d, d+1, d+2, d+3], 0)
                masks.append(m)
                if "cell_1" in i:
                    np.put(m2, [di], 0)
                    masks.append(m2)
                else:
                    d = di * 2
                    np.put(m2, [d, d+1], 0)
                    masks.append(m2)
            info[i].append(masks)

        else:
            # print("here")
            N = info[i][1]
            m = np.ones(N, dtype=bool)
            dropN = int(N*p)
            dropInd = np.random.choice(N, dropN, replace=False)
            m[dropInd] = 0
            info[i].append(m)

    return info


if __name__ == '__main__':
    warnings.filterwarnings('ignore')

    # model = ClientModel(1, 0.001, 10, 'H')  # intitialize the model

    # # Build the model
    # # (batch_size, height, width, channels) , batch size is dynamic
    # model.build(input_shape=(None, 28, 28, 1))

    # # Print out number of parameters
    # # print(f"Number of parameters: {model.count_params()}")
    # print(model.summary())

    # # Load in MNIST data
    # mnist = tf.keras.datasets.mnist
    # # download may take a while
    # (x_train, y_train), (x_test, y_test) = mnist.load_data()
    # x_train, x_test = x_train / 255.0, x_test / 255.0

    # # Fix the shape to be 28x28x1
    # x_train = np.reshape(x_train, (-1, 28, 28, 1))
    # x_test = np.reshape(x_test, (-1, 28, 28, 1))

    # # one hot encode the labels
    # y_train = tf.keras.utils.to_categorical(y_train, 10)
    # y_test = tf.keras.utils.to_categorical(y_test, 10)

    # # features = tf.random.normal([32, 28, 28, 1])
    # # labels = tf.random.normal([32, 10])

    # # Compile the model
    # model.compile(optimizer='adam',
    #               loss='categorical_crossentropy',
    #               metrics=['accuracy'])

    # print(f"-----------------------------COMPILED THE MODEL-----------------------------")

    # # Train the model
    # # model.fit(features, labels, epochs=5)
    # model.fit(x_train, y_train, epochs=5)
########################################################################################################

    # # main for low model

    modelH = ClientModel(1, 0.001, 10, 'H')  # intitialize the model
    modelH.build(input_shape=(None, 28, 28, 1))
    modelH.compile(optimizer='adam',
                   loss='categorical_crossentropy',
                   metrics=['accuracy'])
    layer_names = ['conv1', 'conv_last', 'dense1']
    dropout = 0.5
    # print(modelH.layers[2].layers[0].name)
    # drop_out_layer_info = get_dropout_layer_info(modelH, layer_names)
    # print(drop_out_layer_info)
    masks = create_masks(modelH, layer_names, dropout)
    # print(masks)
    modelL = ClientModel(1, 0.001, 10, 'L', masks)  # intitialize the model

    # build the model
    modelL.build(input_shape=(None, 28, 28, 1))

    # Print out number of parameters
    # print(f"Number of parameters: {model.count_params()}")
    print(modelL.summary())

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
    modelL.compile(optimizer='adam',
                   loss='categorical_crossentropy',
                   metrics=['accuracy'])

    print(f"-----------------------------COMPILED THE MODEL-----------------------------")

    # Train the model
    # model.fit(features, labels, epochs=5)
    modelL.fit(x_train, y_train, epochs=5)

    # # # Evaluate the model
    # # # model.evaluate(x_test, y_test)
    # # modelL.evaluate(x_test, y_test)

    # # # Save the model
    # # # model.save('mnist_model.h5')
