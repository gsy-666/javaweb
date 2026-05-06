<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'

const container = ref<HTMLElement | null>(null)
let animationFrameId: number

onMounted(() => {
  if (!container.value) return

  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  container.value.appendChild(canvas)

  let width = window.innerWidth
  let height = window.innerHeight
  canvas.width = width
  canvas.height = height

  // Nodes for logistics network
  const nodes: { x: number, y: number, vx: number, vy: number, active: boolean, id: number }[] = []
  const maxNodes = 60

  for (let i = 0; i < maxNodes; i++) {
    nodes.push({
      id: i,
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      active: Math.random() > 0.8
    })
  }

  let time = 0

  const handleResize = () => {
    width = window.innerWidth
    height = window.innerHeight
    canvas.width = width
    canvas.height = height
  }
  window.addEventListener('resize', handleResize)

  const draw = () => {
    ctx.clearRect(0, 0, width, height)
    time += 0.01

    ctx.lineWidth = 1
    
    // Update and draw nodes
    for (let i = 0; i < nodes.length; i++) {
      const node = nodes[i]
      node.x += node.vx
      node.y += node.vy
      
      // Bounce
      if (node.x < 0 || node.x > width) node.vx *= -1
      if (node.y < 0 || node.y > height) node.vy *= -1

      // Randomly change active state representing incoming repairs
      if (Math.random() < 0.001) node.active = !node.active

      // Draw node
      ctx.beginPath()
      ctx.arc(node.x, node.y, node.active ? 4 : 2, 0, Math.PI * 2)
      ctx.fillStyle = node.active ? 'rgba(56, 189, 248, 0.9)' : 'rgba(99, 102, 241, 0.4)'
      ctx.fill()
      
      if (node.active) {
        ctx.beginPath()
        ctx.arc(node.x, node.y, 8 + Math.sin(time * 5) * 2, 0, Math.PI * 2)
        ctx.strokeStyle = 'rgba(56, 189, 248, 0.3)'
        ctx.stroke()
      }

      // Draw connections
      for (let j = i + 1; j < nodes.length; j++) {
        const other = nodes[j]
        const dx = node.x - other.x
        const dy = node.y - other.y
        const dist = Math.sqrt(dx * dx + dy * dy)

        if (dist < 180) {
          ctx.beginPath()
          ctx.moveTo(node.x, node.y)
          ctx.lineTo(other.x, other.y)
          
          const opacity = (1 - dist / 180) * 0.2
          ctx.strokeStyle = `rgba(99, 102, 241, ${opacity})`
          if (node.active || other.active) {
            ctx.strokeStyle = `rgba(56, 189, 248, ${opacity * 2})`
          }
          ctx.stroke()
        }
      }
    }

    animationFrameId = requestAnimationFrame(draw)
  }

  draw()

  // Cleanup
  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    cancelAnimationFrame(animationFrameId)
    if (container.value && canvas.parentNode === container.value) {
      container.value.removeChild(canvas)
    }
  })
})
</script>

<template>
  <div class="network-background" ref="container">
    <div class="grid-overlay"></div>
  </div>
</template>

<style scoped>
.network-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 0;
  pointer-events: none;
  background: radial-gradient(circle at 50% 0%, #111827 0%, #030712 100%);
}

.grid-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: 50px 50px;
  background-image: 
    linear-gradient(to right, rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,1) 30%, rgba(0,0,0,0) 80%);
  -webkit-mask-image: radial-gradient(ellipse at center, rgba(0,0,0,1) 30%, rgba(0,0,0,0) 80%);
}
</style>
