
const fs = require("fs");

let code = fs.readFileSync("src/views/AdminView.vue", "utf8");

if (!code.includes("Admin3DScene")) {
  code = code.replace(/import UiCard from \x27..\/components\/UiCard.vue\x27;?/g, "import UiCard from \x27../components/UiCard.vue\x27;\nimport Admin3DScene from \x27../components/Admin3DScene.vue\x27;");
}

const newBentoMap = `
      <!-- 3D GeoJSON Map Container -->
      <div class="bento-item glass col-span-4 map-bento">
        <div class="bento-header" style="position:relative; z-index:20; pointer-events:none;">
          <h2 style="text-shadow: 0 2px 10px rgba(0,0,0,0.8); font-size:18px;">全域态势数字孪生中心 <span style="font-size:12px; color:#38bdf8;">Yanshan University 3D Engine</span></h2>
          <div class="bento-tag">3D Engine Active</div>
        </div>
        <div class="map-wrapper">
          <Admin3DScene />
        </div>
      </div>
`;

if (!code.includes("map-bento")) {
  code = code.replace(/<div class="bento-grid">/g, `<div class="bento-grid">\n${newBentoMap}`);

  code = code.replace(/<\/style>/g, `
.map-bento {
  padding: 0;
  border-radius: 24px;
  overflow: hidden;
  height: 500px;
  position: relative;
  box-shadow: 0 20px 40px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.1);
}
.map-bento .bento-header {
  padding: 20px 24px;
  position: absolute;
  top: 0; left: 0; right: 0;
  pointer-events: none;
}
.map-wrapper {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.map-wrapper > * {
  width: 100%;
  height: 100%;
}
</style>`);
}

fs.writeFileSync("src/views/AdminView.vue", code);
console.log("Done");

