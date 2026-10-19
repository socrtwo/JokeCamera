// Preload script for Electron
// Provides a bridge between the web app and Node.js APIs if needed

const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    isElectron: true,
    platform: process.platform
});
