# Master 300 Step Roadmap

## Phase Map

| Phase | Step Range | Focus |
| --- | --- | --- |
| Phase 01 | 1-50 | Foundation and stabilization |
| Phase 02 | 51-100 | Retrieval and behavior context |
| Phase 03 | 101-150 | Karoo and testing lab |
| Phase 04 | 151-200 | Programming cube and orchestrator |
| Phase 05 | 201-250 | Cloud twin and tunnel continuity |
| Phase 06 | 251-300 | Epoch operations and sustained delivery |

## Checkpoint 01: Steps 1-25

1. Record the current running processes and ports.
2. Record the live bridge command line and source path.
3. Record the live shipper command line and source path.
4. Record the live house inference command line and source path.
5. Record the live topology sidecar command line and source path.
6. Map the current public GUI files and their roles.
7. Map the current API routes and their subsystem owners.
8. List all SQLite tables actively used by chat, retrieval, and shipping.
9. Inventory the current logs and their update cadence.
10. Capture the latest Karoo candidate and report artifacts.
11. Capture the latest system test record.
12. Capture the latest benchmark record.
13. Capture the latest missed-message relay backlog.
14. Capture the latest global todo backlog.
15. Capture the latest logic shipment backlog.
16. Identify the current bridge route behaviors for chat, planning, and build.
17. Confirm which routes are real model calls versus hardcoded summaries.
18. Record the current GUI lock constraints.
19. Record the current mutation guardrails.
20. Document where the cloudflare URL is stored.
21. Document where cloud destinations are stored.
22. Document where user topology summaries are stored.
23. Document where feedback likes and dislikes are stored.
24. Document where retrieval lenses are stored.
25. Publish checkpoint 01 notes and freeze the baseline map.

## Checkpoint 02: Steps 26-50

26. Collect all `database is locked` errors by subsystem.
27. Group lock errors by read path versus write path.
28. Group lock errors by bridge, lens, shipper, and feedback path.
29. Count how many locks occur during feedback submission.
30. Count how many locks occur during benchmark writes.
31. Count how many locks occur during chat memory writes.
32. Count how many locks occur during prefetch writes.
33. Count how many locks occur during topology writes.
34. Identify whether concurrent writers are bridge, shipper, and sidecar together.
35. Identify whether any polling loop is amplifying writer pressure.
36. Identify whether feedback spam triggers clustered lock bursts.
37. Identify which operations can be moved to queued writes later.
38. Identify which tables require higher write priority.
39. Draft a writer-funnel design note.
40. Draft a benchmark-protection design note.
41. Draft a feedback-write protection design note.
42. Draft a chat-memory protection design note.
43. Draft a Karoo-write protection design note.
44. Add the stabilization notes to the blueprint pack.
45. Create the testing-lab subsection list.
46. Create the checkpoint report naming convention.
47. Create the epoch evidence naming convention.
48. Create the route evidence naming convention.
49. Review all phase 01 artifacts for consistency.
50. Publish checkpoint 02 and close phase 01.

## Checkpoint 03: Steps 51-75

51. Confirm the current lens route classifier inputs.
52. Confirm the current token budget rules for each route.
53. Confirm the current query expansion rules.
54. Confirm the current trust weighting rules.
55. Confirm the current route-fit weighting rules.
56. Confirm the current evidence sufficiency rules.
57. Confirm the current top retrieval sources for chat.
58. Confirm the current top retrieval sources for planning.
59. Confirm the current top retrieval sources for build.
60. Confirm the current code-source ranking path for build.
61. Separate nominal user context from behavioral context.
62. Define the nominal context fields to keep small.
63. Define the behavior-card cap at five cards.
64. Define the tie-break rule when many cards are equal.
65. Define the random-tie selection rule using a stable seed.
66. Define the liked-intent card shape.
67. Define the disliked-intent card shape.
68. Define the related-db-logic card shape.
69. Define the markov-hint card shape.
70. Define the card weighting formula.
71. Confirm that the behavior pack stays route-aware.
72. Confirm that the behavior pack does not replace evidence cards.
73. Confirm that the behavior pack does not exceed prompt limits.
74. Confirm that nominal context remains compact and readable.
75. Publish checkpoint 03 and freeze the behavior-pack contract.

## Checkpoint 04: Steps 76-100

76. Add state snapshots for current chat intent.
77. Add state transitions from one chat intent to the next.
78. Add summary-15 cards for each state snapshot.
79. Add route keys to state transitions.
80. Add stable query token slices to state snapshots.
81. Add retrieval-side access to recent state transitions.
82. Add top markov-hint extraction for repeated next intents.
83. Add behavior pack construction from feedback, related logic, and markov hints.
84. Add nominal user context extraction from topology profile.
85. Add the behavior pack to the lens payload.
86. Add the nominal context to the lens payload.
87. Add the state key to the lens payload.
88. Add the new pack to the stored lens metadata.
89. Add tests for empty behavior-pack fallback.
90. Add tests for all-equal tie randomization.
91. Add tests for like-heavy contexts.
92. Add tests for dislike-heavy contexts.
93. Add tests for build-route behavior-pack composition.
94. Add tests for planning-route behavior-pack composition.
95. Add tests for chat-route behavior-pack composition.
96. Confirm the lens helper compiles.
97. Confirm the new retrieval tables migrate safely.
98. Confirm the system remains additive and GUI-safe.
99. Publish checkpoint 04 and close phase 02.
100. Freeze the compact behavior context design.

## Checkpoint 05: Steps 101-125

101. Inventory all current smoke-test entrypoints.
102. Inventory all current benchmark capture paths.
103. Inventory all current epoch-related records.
104. Inventory all current Karoo approval records.
105. Inventory all current topology chunk records.
106. Define the testing lab smoke section.
107. Define the testing lab system section.
108. Define the testing lab benchmark section.
109. Define the testing lab behavioral section.
110. Define the testing lab epoch section.
111. Define the testing lab chaos section.
112. Define report outputs for each lab subsection.
113. Define daily lab run outputs.
114. Define 25-step checkpoint lab outputs.
115. Define epoch promotion lab outputs.
116. Define route-specific replay tests.
117. Define timeout replay tests.
118. Define thin-response replay tests.
119. Define DB-lock replay tests.
120. Define cloud-tunnel-down replay tests.
121. Define GUI-preservation verification tests.
122. Define build-route realism verification tests.
123. Define planning-route completeness verification tests.
124. Define chat warmth and directness verification tests.
125. Publish checkpoint 05 and freeze the lab subsection map.

## Checkpoint 06: Steps 126-150

126. Confirm Karoo chunk refresh behavior.
127. Confirm Karoo candidate generation behavior.
128. Confirm Karoo report generation behavior.
129. Confirm Karoo checkpoint capture behavior.
130. Confirm Karoo comparison count expectations.
131. Confirm the current compare-three-options intent.
132. Identify where successful ledger logic should join comparisons.
133. Identify where local best-known chunks should join comparisons.
134. Identify where rejected candidate memory should join comparisons.
135. Define Karoo comparator input set A.
136. Define Karoo comparator input set B.
137. Define Karoo comparator input set C.
138. Define the comparator scoring note format.
139. Define the actor-critic stop rule format.
140. Define the promotion-gate handoff format.
141. Define the lab report handoff for Karoo runs.
142. Confirm the live build route still contains a hardcoded summary path.
143. Draft the real build-route handoff requirements.
144. Draft the route-unification requirements between bridge and Karoo.
145. Draft the proof contract for build-route autonomy.
146. Add Karoo findings to the notes pack.
147. Add Karoo comparator tasks to the phase 04 queue.
148. Publish checkpoint 06 and close phase 03.
149. Freeze the testing-lab and Karoo comparison interfaces.
150. Mark phase 03 ready for execution work.

## Checkpoint 07: Steps 151-175

151. Define the programming cube mission.
152. Define the pattern registry purpose.
153. Define the template-success database purpose.
154. Define the code-block reuse registry purpose.
155. Define the project-similarity scoring purpose.
156. Define the orchestrator purpose.
157. Define the project-type classifier input set.
158. Define the code-family similarity input set.
159. Define the reuse-confidence metric.
160. Define the verification-required threshold.
161. Define the never-autoverify threshold.
162. Define the reuse-card summary format.
163. Define the project archetype summary format.
164. Define the code-block lineage summary format.
165. Define the compile-proof requirement for reused blocks.
166. Define the test-proof requirement for reused blocks.
167. Define the benchmark-proof requirement for reused blocks.
168. Define the Karoo comparator handoff for candidate reuse.
169. Define the retrieval handoff for successful code blocks.
170. Define the ledger handoff for approved reusable patterns.
171. Define the orchestrator input contract.
172. Define the orchestrator output contract.
173. Define the orchestrator stop condition.
174. Define the orchestrator rollback condition.
175. Publish checkpoint 07 and freeze the programming cube schemas.

## Checkpoint 08: Steps 176-200

176. Define how the programming cube consumes topology trees.
177. Define how the programming cube consumes retrieval cards.
178. Define how the programming cube consumes successful code cards.
179. Define how the programming cube consumes disliked-intent lessons.
180. Define how the programming cube consumes checkpoint evidence.
181. Define how the orchestrator requests Karoo review.
182. Define how the orchestrator requests lab verification.
183. Define how the orchestrator records rejected patterns.
184. Define how the orchestrator records approved patterns.
185. Define how the orchestrator handles uncertain similarity.
186. Define how the orchestrator handles clear similarity.
187. Define how the orchestrator handles no-match cases.
188. Define the first programming-cube smoke test.
189. Define the first programming-cube benchmark.
190. Define the first programming-cube epoch variable.
191. Define the first programming-cube checkpoint report.
192. Define the first programming-cube rollback report.
193. Define the first programming-cube success report.
194. Add programming-cube notes to the blueprint pack.
195. Add programming-cube topology tree notes.
196. Add programming-cube runbook placeholders.
197. Publish checkpoint 08 and close phase 04.
198. Freeze the programming-cube interfaces.
199. Mark the orchestrator as blueprint-ready.
200. Mark the cube as ready for future code execution work.

## Checkpoint 09: Steps 201-225

201. Confirm the current Cloudflare URL file behavior.
202. Confirm the current shipper destination table behavior.
203. Confirm the current uplink and ledger URL fields.
204. Define the cloud twin mission as mirrored persistence.
205. Define the cloud twin scope as hashes and approved metadata first.
206. Define the cloud twin bootstrap prerequisites.
207. Define the Oracle VM bootstrap sequence.
208. Define the Oracle VM package list.
209. Define the Oracle VM service list.
210. Define the persistent Cloudflare tunnel role.
211. Define the tunnel watch role.
212. Define the tunnel rebind role.
213. Define the twin recovery role.
214. Define the twin sync packet scope.
215. Define the twin sync rejection scope.
216. Define the twin replay scope.
217. Define the twin heartbeat scope.
218. Define the twin health scope.
219. Define the twin missed-message relay scope.
220. Define the twin public-link report scope.
221. Define the twin checkpoint restore scope.
222. Define the twin trust model.
223. Define the twin approval model.
224. Define the twin rollback model.
225. Publish checkpoint 09 and freeze the twin contract.

## Checkpoint 10: Steps 226-250

226. Add a local tunnel-watch table.
227. Add local tunnel health probing.
228. Add local tunnel URL change detection.
229. Add local tunnel relay message when link changes.
230. Add local tunnel relay message when link recovers.
231. Add local tunnel relay message when link goes down.
232. Add local tunnel status endpoint.
233. Define the Oracle-side mirror of the local tunnel status.
234. Define the Oracle-side restore packet.
235. Define the Oracle-side replay packet.
236. Define the Oracle-side sync checkpoint.
237. Define the Oracle-side startup runbook.
238. Define the Oracle-side shutdown runbook.
239. Define the Cloudflare credential placement plan.
240. Define the Oracle auth placement plan.
241. Define the notification placement plan.
242. Define the credential rotation plan.
243. Define the tunnel token rotation plan.
244. Define the mail credential rotation plan.
245. Add the twin operations notes to the blueprint pack.
246. Add the twin topology tree notes.
247. Add the twin checkpoint report template.
248. Publish checkpoint 10 and close phase 05.
249. Freeze the twin continuity design.
250. Mark phase 05 ready for credential-enabled deployment work.

## Checkpoint 11: Steps 251-275

251. Define the daily epoch operations cadence.
252. Define the checkpoint cadence.
253. Define the benchmark cadence.
254. Define the tunnel-watch cadence.
255. Define the Karoo review cadence.
256. Define the behavior-pack review cadence.
257. Define the DB-lock incident review cadence.
258. Define the route-quality review cadence.
259. Define the build-route unification review cadence.
260. Define the twin continuity review cadence.
261. Define the runbook directory structure.
262. Define the operations dashboard structure.
263. Define the phase-completion scorecard structure.
264. Define the checkpoint scorecard structure.
265. Define the epoch scorecard structure.
266. Define the rollback scorecard structure.
267. Define the promotion scorecard structure.
268. Define the recovery scorecard structure.
269. Define the team-hand-off note structure.
270. Define the notes-mirroring handoff as a final post-system step.
271. Define the stable README update cadence.
272. Define the blueprint refresh cadence.
273. Define the risk register refresh cadence.
274. Define the source-of-truth docs list.
275. Publish checkpoint 11 and freeze operations structure.

## Checkpoint 12: Steps 276-300

276. Re-read all six phase readmes for consistency.
277. Re-read the integration recommendations for drift.
278. Re-read the testing-lab blueprint for gaps.
279. Re-read the epoch-upgrade blueprint for gaps.
280. Re-read the topology trees for missing subsystems.
281. Verify every phase has a clear exit criteria list.
282. Verify every checkpoint has a publish step.
283. Verify the twin remains local-primary, not migration-primary.
284. Verify the behavior pack remains capped at five cards.
285. Verify nominal context remains separate from behavioral context.
286. Verify the build-route realism task is still explicitly queued.
287. Verify DB lock mitigation remains the top reliability task.
288. Verify tunnel continuity remains scoped to recovery and persistence.
289. Verify notes mirroring is deferred to the final stage.
290. Verify Oracle and Cloudflare credentials are only needed for phase 05 execution.
291. Verify no live GUI mutation is required by this roadmap pack.
292. Verify no code-generation autonomy is promoted without proof.
293. Verify Karoo stays proposal-first until lab proof is stable.
294. Verify the testing lab supports timeout replay and tunnel replay.
295. Verify the blueprint pack is additive and navigable.
296. Verify the roadmap can be executed incrementally.
297. Publish checkpoint 12 and close phase 06.
298. Mark the blueprint pack as today's authoritative planning artifact.
299. Hand off tomorrow's credential-dependent work to phase 05.
300. Start execution from phase 01 or the highest approved unfinished checkpoint.
