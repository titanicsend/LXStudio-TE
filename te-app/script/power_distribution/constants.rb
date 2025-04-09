MAX_CURRENT_PER_LED = 0.045
SCALED_MAX_CURRENT_PER_CIRCUIT = 250 * MAX_CURRENT_PER_LED

LINE_OVERAGE_BUFFER_PERCENT = 0.05
MAX_CONTROLLERS_PER_JUNCTION_BOX = 4
MAX_CHANNELS_PER_CONTROLLER = 8
PANEL_TYPE_LIT = 'lit'
PANEL_TYPE_SOLID = 'solid'

AC_POWER_OUTLET_BANKS = 4
# At each vertex, there's a bank of 20 outlets. Each pair of outlets
# is on one circuit, so we need to ensure there's no more than one
# junction box per circuit.
AC_POWER_OUTLET_BANK_VERTICES = [25, 26, 69, 75]

# Power outlet banks aren't actually at vertices. They're off to either side
# (port or starboard) of the generator and ice lounge. So, call that 6 feet
# offset from the vertex we've assigned it to. Past that, add another 6 feet
# as wiggle room. We can always cut these cables down to length later, and
# extra length is not the end of the world.
AC_POWER_WIGGLE_FEET = 12

MAX_CONTROLLER_DISTANCE_SIGNAL_TO_FIRST_PIXEL_FEET = 7.1

# Mirror this: https://docs.google.com/spreadsheets/d/1C7VPybckgH9bWGxwtgMN_Ij1T__c5qc-k7yIhG-592Y/edit#gid=1572666054
EDGE_BUILD_PRIORITY_LOW = 'Low'

# See status here: https://docs.google.com/spreadsheets/d/1dQS0hEfm1FXL7tJPlmNtxtZmMggipbxqrDQaaxlxFmo/edit#gid=0
# And actually, some of these are recategorized to medium/high now.
BUILT_LOW_PRIORITY_EDGES = [
    '25-84',
    '25-88',
    '27-109',
    '27-110',
    '52-75',
    '58-96',
    '73-81',
    '73-92',
    '84-86',
    '88-110'
]
