import os
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../..")
ITEM = os.path.join(ROOT, "af9-core/src/main/resources/assets/af9/textures/item")
SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), "optical_src")
FILES = ['wafers/transmuted_neutronium_wafer', 'wafers/broken_transmuted_neutronium_wafer',
         'wafers/contaminated_transmuted_neutronium_wafer', 'wafers/tpu_wafer', 'wafers/memristor_wafer',
         'chips/tpu_chip', 'chips/memristor_chip', 'chips/contaminated_tpu_chip', 'chips/contaminated_memristor_chip']
