// 棋盘逻辑组合式函数
export function useBoard() {
  // 创建指定大小的棋盘
  const createBoard = (size) => {
    const board = [];
    for (let i = 0; i < size; i++) {
      board[i] = [];
      for (let j = 0; j < size; j++) {
        board[i][j] = 0; // 0表示空位置
      }
    }
    return board;
  };

  // 检查落子位置是否合法
  const isValidMove = (board, row, col) => {
    const size = board.length;
    return row >= 0 && row < size && col >= 0 && col < size && board[row][col] === 0;
  };

  // 获取棋盘状态
  const getBoardState = (board) => {
    return JSON.parse(JSON.stringify(board));
  };

  // 更新棋盘状态
  const updateBoard = (board, row, col, player) => {
    if (isValidMove(board, row, col)) {
      board[row][col] = player;
      return true;
    }
    return false;
  };

  return {
    createBoard,
    isValidMove,
    getBoardState,
    updateBoard
  };
}