import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const unreadMessageCount = ref(0)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setUnreadCount(count: number) {
    unreadMessageCount.value = count
  }

  return {
    sidebarCollapsed,
    unreadMessageCount,
    toggleSidebar,
    setUnreadCount,
  }
})
