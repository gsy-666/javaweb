<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'

const props = withDefaults(defineProps<{
  size?: number
  pupilSize?: number
  maxDistance?: number
  eyeColor?: string
  pupilColor?: string
  isBlinking?: boolean
  forceLookX?: number | undefined
  forceLookY?: number | undefined
}>(), {
  size: 48,
  pupilSize: 16,
  maxDistance: 10,
  eyeColor: 'white',
  pupilColor: 'black',
  isBlinking: false
})

const eyeRef = ref<HTMLElement | null>(null)
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
  
  if (!eyeRef.value) return { x: 0, y: 0 }
  
  const rect = eyeRef.value.getBoundingClientRect()
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
    ref="eyeRef"
    class="eyeball"
    :style="{
      width: `${size}px`,
      height: isBlinking ? '2px' : `${size}px`,
      backgroundColor: eyeColor,
    }"
  >
    <div
      v-if="!isBlinking"
      class="pupil-inner"
      :style="{
        width: `${pupilSize}px`,
        height: `${pupilSize}px`,
        backgroundColor: pupilColor,
        transform: `translate(${pupilPosition.x}px, ${pupilPosition.y}px)`,
      }"
    ></div>
  </div>
</template>

<style scoped>
.eyeball {
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.15s ease;
}

.pupil-inner {
  border-radius: 9999px;
  transition: transform 0.1s ease-out;
}
</style>
