#! /usr/bin/env python3

mapping_ints = {
   11: 121,
   10: 122,
  117: 123,
  124: 130,
  116: 143,
    9: 142,
   12: 141,
   79: 161,
   78: 162,
  129: 163,
  126: 170,
  125: 183,
   60: 182,
   65: 181,

   98: 221,
  100: 222,
  115: 223,
  113: 224,
  112: 244,
  114: 243,
   83: 242,
   85: 241,
   80: 261,
   97: 262,
  128: 263,
   91: 264,
   89: 284,
  127: 283,
   93: 282,
   67: 281,

   99: 321,
   26: 322,
   28: 323,
  109: 330,
   27: 343,
   25: 342,
   84: 341,
   96: 361,
   75: 362,
   73: 363,
   81: 370,
   70: 383,
   69: 382,
   90: 381,

   44: 400,
  101: 421,
  102: 422,
  111: 423,
  110: 443,
   88: 442,
   86: 441,
   57: 450,
   58: 461,
   52: 462,
   92: 463,
   82: 483,
   51: 482,
   47: 481,
  
   39: 521,
  121: 522,
  119: 530,
  120: 542,
   43: 541,
   36: 561,
   56: 562,
   46: 563,
   55: 564,
   54: 584,
   45: 583,
   50: 582,
   37: 581,

   31: 621,
   42: 641,
  122: 670,
  123: 671,

   33: 721,
  118: 730,
   38: 741,

   30: 830,
}

mapping = {str(key): str(value) for key, value in mapping_ints.items()}

# Check for dupes
assert len(set(mapping.keys())) == len(mapping)
assert len(set(mapping.values())) == len(mapping)

def change_file(filename, linemapper):
  fd = open(filename, "r")
  lines = fd.readlines()
  fd.close()
  fd = open("new-" + filename, "w")
  for line in lines:
    fd.write(linemapper(line))
  fd.close()
  print(f"Updated {filename}")

def vertexes_lm(line):
  tokens = line.split("\t")
  tokens[0] = mapping[tokens[0]]
  return "\t".join(tokens)

def map_edge(edge):
  if edge == "Controller":
    return edge
  v0, v1 = edge.split("-")
  m0 = mapping[v0]
  m1 = mapping[v1]
  i0 = int(m0)
  i1 = int(m1)
  if i0 < i1:
    return m0 + "-" + m1
  else:
    return m1 + "-" + m0

def edges_lm(line):
  tokens = line.split("\t")
  tokens[0] = map_edge(tokens[0])
  return "\t".join(tokens)

def panels_lm(line):
  tokens = line.split("\t")
  tokens[1] = map_edge(tokens[1])
  tokens[2] = map_edge(tokens[2])
  tokens[3] = map_edge(tokens[3])
  v0, v1 = tokens[4].split("->")
  tokens[4] = mapping[v0] + "->" + mapping[v1]
  return "\t".join(tokens)

change_file("vertexes.txt", vertexes_lm, False)
change_file("edges.txt", edges_lm, False)
change_file("panels.txt", panels_lm, False)
