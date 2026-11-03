# Rendering performance (0.10.4)

Machine faces are cached by texture. Opaque materials are submitted before translucent ones, reducing buffer changes without removing authored faces. Vertex positions and normals use reusable vectors and the bulk VertexConsumer entry point. Fluids share the evaluated machine pose. Mech normals and hidden-bone checks are evaluated per bone instead of per face.

Cable tessellation is retained across identical network snapshots. Only changed routes rebuild a mesh; lighting refreshes at most once per visible cable per game tick. Distance and frustum culling discard offscreen routes. Logout clears caches. Route previews still update interactively. Blast furnaces now use their existing full-structure bounding box for frustum culling instead of always rendering offscreen.

Glider cube/mesh polygons, UVs and normals are compiled once per resource load. Render frames retain animation, dye, mirror and limb filtering, without reparsing the element JSON. Resource reload invalidation uses the existing GliderScene.invalidate hook.

Validation: 32 unit tests; machine client sequence covers all ten machine rigs in the world and as items; cable client sequence covers four air/floor routes and two camera views; cached glider geometry checks cover carry, flight, dye and rider-limb filtering. Test clients disconnect before stopping.

The same machineQa camera sequence measured 3.800 ms before and 1.192 ms after per drawPose call (aggregate CPU time; 68.6% lower). This excludes animation pose evaluation and other game work, includes material buffer acquisition, and is not an FPS measurement. Sampling weights vary with frame rate. Raw counts and totals are recorded in docs/verification/performance-0.10.4.json. No server simulation or gameplay changes.
