<template>
  <div class="game-controls-container">
    <div class="game-status" :class="{ 'win': gameOver && winner }">
      <template v-if="gameOver">
        <span v-if="winner === 1">黑棋获胜！</span>
        <span v-else-if="winner === 2">白棋获胜！</span>
      </template>
      <template v-else>
        <span v-if="currentPlayer === 1">黑棋回合</span>
        <span v-else>白棋回合</span>
      </template>
    </div>
    
    <div class="game-controls">
      <button class="control-btn" @click="handleRestart">重新开始</button>
      <button class="control-btn" @click="handleUndo">悔棋</button>
    </div>
    
    <div class="game-settings">
      <div class="setting-group">
        <label class="setting-label">游戏模式：</label>
        <select class="setting-select" :value="gameMode" @change="handleModeChange">
          <option value="human">人人对战</option>
          <option value="ai">人机对战</option>
        </select>
      </div>
      
      <div class="setting-group" v-if="gameMode === 'ai'">
        <label class="setting-label">AI难度：</label>
        <select class="setting-select" :value="difficulty" @change="handleDifficultyChange">
          <option value="1">简单</option>
          <option value="2">中等</option>
          <option value="3">困难</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GameControls',
  props: {
    currentPlayer: {
      type: Number,
      required: true
    },
    gameOver: {
      type: Boolean,
      default: false
    },
    winner: {
      type: Number,
      default: null
    },
    gameMode: {
      type: String,
      default: 'human'
    },
    difficulty: {
      type: Number,
      default: 1
    }
  },
  methods: {
    handleRestart() {
      this.$emit('restart');
    },
    handleUndo() {
      this.$emit('undo');
    },
    handleModeChange(event) {
      this.$emit('change-mode', event.target.value);
    },
    handleDifficultyChange(event) {
      this.$emit('change-difficulty', parseInt(event.target.value));
    }
  }
};
</script>

<style scoped>
.game-controls-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.game-status {
  font-size: 18px;
  font-weight: bold;
  color: #333333;
  text-align: center;
}

.game-controls {
  display: flex;
  gap: 10px;
}

.control-btn {
  padding: 8px 16px;
  background-color: #42a5f5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.control-btn:hover {
  background-color: #1976d2;
}

.game-settings {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 300px;
}

.setting-group {
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: center;
  gap: 10px;
}

.setting-label {
  font-size: 14px;
  color: #333333;
}

.setting-select {
  padding: 4px 8px;
  border: 1px solid #dddddd;
  border-radius: 4px;
  font-size: 14px;
  background-color: #ffffff;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .game-controls {
    flex-direction: column;
    width: 100%;
    align-items: center;
  }
  
  .control-btn {
    width: 120px;
    text-align: center;
  }
  
  .setting-group {
    flex-direction: column;
    gap: 5px;
  }
}
</style>