<template>
  <div class="board" @click="handleClick">
    <div 
      v-for="(row, rowIndex) in boardSize" 
      :key="rowIndex" 
      class="board-row"
    >
      <div 
        v-for="(col, colIndex) in boardSize" 
        :key="colIndex" 
        class="board-cell"
        :data-row="rowIndex"
        :data-col="colIndex"
      >
        <ChessPiece 
          v-if="board[rowIndex][colIndex]" 
          :color="board[rowIndex][colIndex] === 1 ? 'black' : 'white'"
        />
      </div>
    </div>
  </div>
</template>

<script>
import ChessPiece from './ChessPiece.vue';

export default {
  name: 'Board',
  components: {
    ChessPiece
  },
  props: {
    board: {
      type: Array,
      required: true
    },
    boardSize: {
      type: Number,
      default: 15
    },
    gameOver: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    handleClick(event) {
      if (this.gameOver) {
        return;
      }
      
      const cell = event.target.closest('.board-cell');
      if (cell) {
        const row = parseInt(cell.dataset.row);
        const col = parseInt(cell.dataset.col);
        this.$emit('place-piece', row, col);
      }
    }
  }
};
</script>

<style scoped>
.board {
  display: grid;
  grid-template-columns: repeat(15, 30px);
  grid-template-rows: repeat(15, 30px);
  gap: 0;
  border: 2px solid #8b4513;
  background-color: #f5f5dc;
  margin: 0 auto 20px;
}

.board-cell {
  width: 30px;
  height: 30px;
  border: 1px solid #8b4513;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .board {
    grid-template-columns: repeat(15, 25px);
    grid-template-rows: repeat(15, 25px);
  }
  
  .board-cell {
    width: 25px;
    height: 25px;
  }
}

@media (max-width: 480px) {
  .board {
    grid-template-columns: repeat(15, 20px);
    grid-template-rows: repeat(15, 20px);
  }
  
  .board-cell {
    width: 20px;
    height: 20px;
  }
}
</style>