
<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from "vue";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { CSS2DRenderer, CSS2DObject } from "three/examples/jsm/renderers/CSS2DRenderer.js";
import gsap from "gsap";
import * as d3 from "d3-geo";
// 引入 api 拿真实数据
import { api } from "../api";

const container = ref<HTMLElement | null>(null);
const cssContainer = ref<HTMLElement | null>(null);

let scene: THREE.Scene, camera: THREE.PerspectiveCamera, renderer: THREE.WebGLRenderer, cssRenderer: CSS2DRenderer, controls: OrbitControls;
let animationFrameId: number;
const objectsToAnimate: { obj: THREE.Object3D, type: string, params?: any }[] = [];

onMounted(async () => {
  if (!container.value || !cssContainer.value) return;

  scene = new THREE.Scene();

  camera = new THREE.PerspectiveCamera(45, container.value.clientWidth / container.value.clientHeight, 0.1, 3000);
  camera.position.set(0, 350, 400);

  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
  renderer.setSize(container.value.clientWidth, container.value.clientHeight);
  renderer.setPixelRatio(window.devicePixelRatio);
  container.value.appendChild(renderer.domElement);

  cssRenderer = new CSS2DRenderer();
  cssRenderer.setSize(container.value.clientWidth, container.value.clientHeight);
  cssRenderer.domElement.style.position = "absolute";
  cssRenderer.domElement.style.top = "0";
  cssContainer.value.appendChild(cssRenderer.domElement);

  // 绑定交互事件到 cssRenderer.domElement 上，解决拖拽被拦截的问题
  controls = new OrbitControls(camera, cssRenderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.05;
  controls.maxPolarAngle = Math.PI / 2 - 0.05;

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);
  const dirLight = new THREE.DirectionalLight(0x22d3ee, 2);
  dirLight.position.set(100, 200, 100);
  scene.add(dirLight);

  // 1. 获取本地 GeoJSON 建筑数据
  let geoData: any = { features: [] };
  try {
     const req = await fetch("/ysu-map.json");
     geoData = await req.json();
  } catch(e) {
     console.error("GeoJSON load failed", e);
  }

  // 定义 D3 投影，将真实经纬度映射到 3D 平面的 x 和 z 轴
  const projection = d3.geoMercator().center([119.556, 39.9145]).scale(3500000).translate([0, 0]);

  // 赛博朋克建筑发光材质
  const buildingMaterial = new THREE.ShaderMaterial({
    uniforms: {
      color1: { value: new THREE.Color(0x1e3a8a) }, 
      color2: { value: new THREE.Color(0x38bdf8) }, 
      bboxMin: { value: new THREE.Vector3(-100, 0, -100) },
      bboxMax: { value: new THREE.Vector3(100, 60, 100) }
    },
    vertexShader: `
      varying vec3 vPosition;
      void main() {
        vPosition = position;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
      }
    `,
    fragmentShader: `
      uniform vec3 color1;
      uniform vec3 color2;
      uniform vec3 bboxMin;
      uniform vec3 bboxMax;
      varying vec3 vPosition;
      
      void main() {
        float h = (vPosition.y - bboxMin.y) / (bboxMax.y - bboxMin.y);
        vec3 color = mix(color1, color2, clamp(h, 0.0, 1.0));
        float rim = 1.0 - max(dot(viewMatrix[2].xyz, vec3(0.0, 1.0, 0.0)), 0.0);
        color += vec3(0.1, 0.3, 0.5) * smoothstep(0.6, 1.0, rim);
        gl_FragColor = vec4(color, 0.85);
      }
    `,
    transparent: true,
    side: THREE.DoubleSide
  });

  const group = new THREE.Group();
  scene.add(group);

  // 2. 根据真实地图坐标生成 3D 建筑实体
  geoData.features.forEach((feature: any) => {
    if (feature.geometry.type === "Polygon") {
      const shape = new THREE.Shape();
      const coords = feature.geometry.coordinates[0];
      
      coords.forEach((coord: any, index: number) => {
        const [x, y] = projection(coord) || [0, 0];
        // 经纬度映射: D3 出来的 y 是向下的，在 3D 中我们映射到 -Z 轴
        if (index === 0) shape.moveTo(x, -y);
        else shape.lineTo(x, -y);
      });

      const depth = feature.properties.height || 20;
      const extrudeSettings = {
        depth: depth,
        bevelEnabled: true,
        bevelSegments: 1,
        steps: 1,
        bevelSize: 0.5,
        bevelThickness: 0.5
      };

      const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings);
      geometry.rotateX(Math.PI / 2);
      geometry.translate(0, depth, 0);

      const mesh = new THREE.Mesh(geometry, buildingMaterial);
      group.add(mesh);

      // 发光边缘线条
      const edges = new THREE.EdgesGeometry(geometry);
      const edgeMat = new THREE.LineBasicMaterial({ color: 0x38bdf8, transparent: true, opacity: 0.4 });
      const line = new THREE.LineSegments(edges, edgeMat);
      mesh.add(line);

      // 计算中心点用于挂载标签
      geometry.computeBoundingBox();
      const center = new THREE.Vector3();
      geometry.boundingBox?.getCenter(center);

      if (feature.properties.name) {
        const div = document.createElement("div");
        div.className = "label-css2d";
        div.textContent = feature.properties.name;
        
        const label = new CSS2DObject(div);
        label.position.set(center.x, depth + 10, center.z);
        mesh.add(label);
      }
      
      mesh.scale.set(1, 0.01, 1);
      gsap.to(mesh.scale, { y: 1, duration: 2, ease: "elastic.out(1, 0.3)", delay: Math.random() * 0.5 });
    }
  });

  // 3. 从后端数据库获取真实的报修工单，并使用同一个坐标系映射到底盘上
  try {
    const res = await api.get("/api/admin/work-orders");
    const list = res.data?.data || [];
    const activeOrders = list.filter((o: any) => o.status !== "CLOSED" && o.status !== "CANCELED");
    
    activeOrders.forEach((o: any) => {
      // 只要有经纬度数据，就渲染真实工单位置
      if (o.lng && o.lat) {
        const projCoords = projection([o.lng, o.lat]);
        if (!projCoords) return;
        const [localX, localY] = projCoords;
        const localZ = -localY; // 与建筑生成的坐标系保持一致
        
        let level = "INFO";
        if (o.priority === 1) level = "WARNING";
        if (o.priority === 2) level = "CRITICAL";

        const title = o.address || (o.areaName ? o.areaName + o.buildingName : o.code) || "报修地点";
        addFaultMarker(localX, 1, localZ, level, title, o.description);
      }
    });
  } catch(e) {
    console.error("Failed to load real work orders from database", e);
  }

  function addFaultMarker(x: number, y: number, z: number, level: string, bName: string, desc: string) {
    let colorNum = 0x3b82f6; 
    let colorStr = "#3b82f6";
    if (level === "WARNING") { colorNum = 0xf59e0b; colorStr = "#f59e0b"; }
    if (level === "CRITICAL") { colorNum = 0xef4444; colorStr = "#ef4444"; }

    // 涟漪基座
    const ringGeo = new THREE.RingGeometry(2, 6, 32);
    const ringMat = new THREE.MeshBasicMaterial({ color: colorNum, transparent: true, opacity: 0.8, side: THREE.DoubleSide });
    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.rotation.x = -Math.PI / 2;
    ring.position.set(x, y, z);
    scene.add(ring);
    objectsToAnimate.push({ obj: ring, type: "ripple" });

    // 光柱
    const pillarGeo = new THREE.CylinderGeometry(0.8, 1.5, 40, 16);
    const pillarMat = new THREE.MeshBasicMaterial({ 
      color: colorNum, 
      transparent: true, 
      opacity: 0.5,
      blending: THREE.AdditiveBlending 
    });
    const pillar = new THREE.Mesh(pillarGeo, pillarMat);
    pillar.position.set(x, y + 20, z);
    scene.add(pillar);

    // 悬浮 HTML 标记面板
    const div = document.createElement("div");
    div.className = "fault-label";
    div.innerHTML = "<div class=\"fault-badge\" style=\"background: " + colorStr + "; box-shadow: 0 0 8px " + colorStr + "\"></div>" + 
                    "<div><b>" + bName + "</b><br><span style=\"opacity:0.8;font-size:11px\">" + (desc || "无描述") + "</span></div>";
    
    div.addEventListener("pointerdown", (e) => e.stopPropagation());
    
    const cssObj = new CSS2DObject(div);
    cssObj.position.set(x, y + 45, z);
    scene.add(cssObj);
  }

  // 底部辅助网格
  const gridHelper = new THREE.GridHelper(800, 40, 0x1e3a8a, 0x1e3a8a);
  gridHelper.position.y = -1;
  gridHelper.material.opacity = 0.2;
  gridHelper.material.transparent = true;
  scene.add(gridHelper);

  // 飞线动画
  const curve = new THREE.CatmullRomCurve3([
    new THREE.Vector3(-100, 30, -50),
    new THREE.Vector3(0, 60, -20),
    new THREE.Vector3(80, 40, 40),
    new THREE.Vector3(120, 20, 80)
  ]);
  const tubeGeo = new THREE.TubeGeometry(curve, 64, 0.6, 8, false);
  const tubeMat = new THREE.MeshBasicMaterial({ color: 0xa78bfa, transparent: true, opacity: 0.4 });
  const tube = new THREE.Mesh(tubeGeo, tubeMat);
  scene.add(tube);
  
  const flyGeo = new THREE.SphereGeometry(2, 16, 16);
  const flyMat = new THREE.MeshBasicMaterial({ color: 0xffffff });
  const flyPoint = new THREE.Mesh(flyGeo, flyMat);
  scene.add(flyPoint);
  objectsToAnimate.push({ obj: flyPoint, type: "fly", params: { t: 0, curve }});

  // 开场摄影机动画
  camera.position.set(0, 600, 50);
  gsap.to(camera.position, {
    y: 220, z: 250, 
    duration: 2.5, 
    ease: "power3.out",
    onUpdate: () => controls.update() 
  });

  window.addEventListener("resize", onWindowResize);
  animate();
});

function getHexColor(colorNum: number) {
  return "#" + colorNum.toString(16).padStart(6, "0");
}
window.setTimeout(() => getHexColor(0), 1000000);

function onWindowResize() {
  if (!camera || !renderer || !container.value) return;
  camera.aspect = container.value.clientWidth / container.value.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(container.value.clientWidth, container.value.clientHeight);
  cssRenderer.setSize(container.value.clientWidth, container.value.clientHeight);
}

function animate() {
  animationFrameId = requestAnimationFrame(animate);
  controls.update();

  objectsToAnimate.forEach((o) => {
    if (o.type === "ripple") {
      o.obj.scale.x += 0.02;
      o.obj.scale.y += 0.02;
      const mesh = o.obj as THREE.Mesh;
      let op = (mesh.material as THREE.Material).opacity;
      op -= 0.01;
      if (op <= 0) {
        o.obj.scale.set(1, 1, 1);
        op = 0.8;
      }
      (mesh.material as THREE.Material).opacity = op;
    } else if (o.type === "fly") {
      o.params.t += 0.005;
      if (o.params.t > 1) o.params.t = 0;
      const point = o.params.curve.getPoint(o.params.t);
      o.obj.position.copy(point);
    }
  });

  renderer.render(scene, camera);
  cssRenderer.render(scene, camera);
}

onBeforeUnmount(() => {
  window.removeEventListener("resize", onWindowResize);
  cancelAnimationFrame(animationFrameId);
  renderer?.dispose();
});
</script>

<template>
  <div class="map-container">
    <div ref="container" class="webgl-layer"></div>
    <div ref="cssContainer" class="css-layer"></div>
  </div>
</template>

<style>
.label-css2d {
  color: #fff;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid #38bdf8;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  backdrop-filter: blur(4px);
  pointer-events: auto;
  cursor: default;
  transition: all 0.3s;
}
.label-css2d:hover {
  background: #38bdf8;
  color: #000;
}
.fault-label {
  color: #fff;
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  pointer-events: auto;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  backdrop-filter: blur(8px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  transition: transform 0.2s;
  min-width: 140px;
}
.fault-label:hover {
  transform: scale(1.05);
  border-color: rgba(255, 255, 255, 0.5);
}
.fault-label * {
  pointer-events: none;
}
.fault-badge {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>

<style scoped>
.map-container {
  width: 100%;
  height: 100%;
  min-height: 480px;
  position: relative;
  overflow: hidden;
  background: radial-gradient(circle at center, #0a0f1d 0%, #03050a 100%);
  border-radius: 16px;
}
.webgl-layer, .css-layer {
  position: absolute;
  inset: 0;
}
</style>

