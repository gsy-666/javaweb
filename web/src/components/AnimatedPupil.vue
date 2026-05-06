<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'

const props = withDefaults(defineProps<{
  size?: number
  maxDistance?: number
  pupilColor?: string
  forceLookX?: number
  forceLookY?: number
}>(), {
  size: 12,
  maxDistance: 5,
  pupilColor: 'black'
})

const pupilRef = ref<HTMLElement | null>(null)
const mouseX = ref(0)
const mouseY = ref(0)

const onMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
})

const pupilPosition = computed(() => {
  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return { x: props.forceLookX, y: props.forceLookY }
  }
  
  if (!pupilRef.value) return { x: 0, y: 0 }
  
  const rect = pupilRef.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  
  const deltaX = mouseX.value - centerX
  const deltaY = mouseY.value - centerY
  const dist = Math.min(Math.sqrt(deltaX * deltaX + deltaY * deltaY), props.maxDistance)
  
  const angle = Math.atan2(deltaY, deltaX)
  return {
    x: Math.cos(angle) * dist,
    y: Math.sin(angle) * dist
  }
})
</script>

<template>
  <div
    ref="pupilRef"
    class="pupil"
    :style="{
      width: `${size}px`,
      height: `${size}px`,
      backgroundColor: pupilColor,
      transform: `translate(${pupilPosition.x}px, ${pupilPosition.y}px)`,
    }"
  ></div>
</template>

<style scoped>
.pupil {
  border-radius: 9999px;
  transition: transform 0.1s ease-out;
}
</style>
