colors = {
    "red": 1,
    "orange": 21,
    "yellow": 42,
    "green": 85,
    "cyan": 127,
    "blue": 170,
    "pink": 234
}

effects = {
    "off": 0,
    "solid": 1,
    "pulse": 2,
    "blink": 3,
    "fast blink": 3,
    "slow blink": 4
}

level = int(float(data.get('level', 10)))
if level > 10:
    level = 10
elif level < 1:
    level = 1

color_name = data.get('color', 'blue').lower()
if color_name in colors:
    color = colors[color_name]
else:
    color = int(color_name)
    
effect = data.get('effect', 'blink').lower()
if effect in effects:
    effect = effects[effect]
else:
    effect = effects['blink']
    
duration = int(float(data.get('duration', 20)))

value = color
value = value + (level * 256)
value = value + (duration * 65536)
value = value + (effect * 16777216)

if effect == 0:
    value = 0
    
#if 'node_ids' in data:
#    node_ids = data.get('node_ids')
#else:
#    node_ids = []
#    for entity in hass.states.entity_ids('zwave'):
#        attributes = hass.states.get(entity).attributes
#        if 'LZW31-SN' in attributes['product_name']:
#            node_ids.append(attributes['node_id'])
#            
#for node_id in node_ids:
#    service_data = {'node_id': node_id, 'parameter': 16, 'size': 4, 'value': int(value)}
#    hass.services.call('zwave', 'set_config_parameter', service_data, False)