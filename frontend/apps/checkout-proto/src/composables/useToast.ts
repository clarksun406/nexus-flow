import { ref } from 'vue'

const message = ref('')
const visible = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

export function useToast() {
  function show(msg: string, duration = 2000) {
    if (timer) {
      clearTimeout(timer)
    }
    message.value = msg
    visible.value = true
    timer = setTimeout(() => {
      visible.value = false
      timer = null
    }, duration)
  }

  function hide() {
    if (timer) {
      clearTimeout(timer)
    }
    visible.value = false
  }

  return {
    message,
    visible,
    show,
    hide,
  }
}
