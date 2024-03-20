import hls4ml
import keras
import pprint

model = keras.saving.load_model('models/out.keras')
config = hls4ml.utils.config_from_keras_model(model)

 # You can print the configuration to see some default parameters
pprint.pprint(model)
pprint.pprint(config)

 # Convert it to a hls project
hls_model = hls4ml.converters.keras_to_hls(config)

#hls_model.build()

#hls4ml.report.read_vivado_report('my-hls-test-sinus')
