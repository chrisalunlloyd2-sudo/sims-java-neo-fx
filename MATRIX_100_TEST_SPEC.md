# 100 Test Matrix Spec

## Purpose

Define the exact `10 x 10` matrix that must pass before Karoo evolves pages or
the system moves back to broader upgrades.

## Dimensions

### 01 Syntax

1. parser accepts file
2. no malformed block structure
3. no invalid tokens
4. no broken string/comment structure
5. required entry function exists
6. required exit symbols exist
7. identifier names compile
8. no duplicate forbidden symbols
9. kernel style rules satisfied
10. syntax result logged

### 02 Imports And Dependencies

11. required imports present
12. forbidden imports absent
13. imports resolve
14. import ordering valid
15. dependency slice not exceeded
16. no circular local dependency introduced
17. dependency ownership preserved
18. external dependency count stable
19. dependency deltas logged
20. result recorded

### 03 Topology Fit

21. page parent correct
22. sibling expectations preserved
23. dependency nodes preserved
24. entry points preserved
25. exit points preserved
26. route owner preserved
27. topology scope unchanged
28. no unauthorized node coupling
29. topology hash recorded
30. result recorded

### 04 Performative Fit

31. performative mode preserved
32. proof target preserved
33. route contract preserved
34. output storage role preserved
35. no hidden side-action introduced
36. allowed action set respected
37. boundary of responsibility preserved
38. performative log emitted
39. contract hash recorded
40. result recorded

### 05 Compile Or Parse

41. compile or parse command succeeds
42. no fatal warnings above threshold
43. build artifacts stable
44. page-local compile time bounded
45. return code zero
46. error stream captured
47. stdout captured
48. retry not needed
49. proof hash emitted
50. result recorded

### 06 Route Fit

51. expected route still exists
52. route payload shape preserved
53. route response shape preserved
54. route latency below threshold
55. route owner unchanged
56. route hook unchanged
57. route proof attached
58. no fallback drift introduced
59. route verdict logged
60. result recorded

### 07 Deficiency Repair

61. deficiency list generated if fail
62. deficiency type classified
63. single-variable repair chosen
64. repair scope stayed local
65. repair removed original deficiency
66. repair did not add new fatal issue
67. repair count within bound
68. repair proof attached
69. repair verdict logged
70. result recorded

### 08 Output Determinism

71. same input gives same structure
72. same page card gives same ownership
73. import set stable across retries
74. route contract stable across retries
75. proof contract stable across retries
76. no random uncontrolled edits
77. file path unchanged
78. content hash drift explained
79. deterministic notes logged
80. result recorded

### 09 Latency And Resource

81. author runtime bounded
82. verifier runtime bounded
83. repair runtime bounded
84. memory use bounded
85. GPU use optional only
86. CPU fallback available
87. no listener starvation observed
88. queue pressure acceptable
89. resource verdict logged
90. result recorded

### 10 Proof And Recording

91. candidate output stored
92. deficiency output stored
93. repair output stored if needed
94. matrix result stored
95. hashes attached
96. timestamps attached
97. page lineage attached
98. promotion gate attached
99. Karoo eligibility attached
100. result recorded

## Pass Rule

A page passes only when:

- all fatal checks pass
- no unresolved deficiency remains
- proof and recording checks are complete

## Batch Rule

A program batch passes only when:

- every page passes its matrix
- page interactions do not break route fit
- Karoo has either no work or only improvement work

## Genetic Performance Overlay

After each batch, record:

- active algorithm id
- weight vector id
- generation id
- matrix pass ratio
- average repair count
- latency profile
- promotion success ratio

Future runs should weigh candidate algorithms using these measured outcomes
rather than static preference alone.
