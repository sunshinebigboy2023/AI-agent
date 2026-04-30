<template>
  <router-view />
</template>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html,
body {
  width: 100%;
  min-height: 100%;
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  color: #172033;
  background: #f4f7fb;
  overflow-x: hidden;
}

button,
textarea {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
}

a {
  color: inherit;
  text-decoration: none;
}

#app {
  min-height: 100vh;
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #eef2f7;
}

::-webkit-scrollbar-thumb {
  background: #b7c2d4;
  border-radius: 8px;
}
</style>
