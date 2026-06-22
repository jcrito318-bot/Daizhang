#include <stdio.h>
#include <stdbool.h>

#define SIZE 9

// 检查在指定位置放置数字是否合法
bool is_valid(int board[SIZE][SIZE], int row, int col, int num) {
    // 检查行
    for (int x = 0; x < SIZE; x++) {
        if (board[row][x] == num) {
            return false;
        }
    }

    // 检查列
    for (int x = 0; x < SIZE; x++) {
        if (board[x][col] == num) {
            return false;
        }
    }

    // 检查3x3宫格
    int startRow = row - row % 3;
    int startCol = col - col % 3;
    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
            if (board[i + startRow][j + startCol] == num) {
                return false;
            }
        }
    }

    return true;
}

// 使用回溯法求解数独
bool solve_sudoku(int board[SIZE][SIZE]) {
    for (int row = 0; row < SIZE; row++) {
        for (int col = 0; col < SIZE; col++) {
            // 找到空位
            if (board[row][col] == 0) {
                // 尝试放置1-9
                for (int num = 1; num <= 9; num++) {
                    if (is_valid(board, row, col, num)) {
                        board[row][col] = num;

                        // 递归求解
                        if (solve_sudoku(board)) {
                            return true;
                        }

                        // 回溯
                        board[row][col] = 0;
                    }
                }
                return false;
            }
        }
    }
    return true;
}

// 打印数独棋盘
void print_board(int board[SIZE][SIZE]) {
    for (int i = 0; i < SIZE; i++) {
        if (i % 3 == 0 && i != 0) {
            printf("- - - - - - - - - - - -\n");
        }
        for (int j = 0; j < SIZE; j++) {
            if (j % 3 == 0 && j != 0) {
                printf("| ");
            }
            if (board[i][j] == 0) {
                printf(". ");
            } else {
                printf("%d ", board[i][j]);
            }
        }
        printf("\n");
    }
}

int main() {
    // 示例数独题目（0表示空格）
    int board[SIZE][SIZE] = {
        {5, 3, 0, 0, 7, 0, 0, 0, 0},
        {6, 0, 0, 1, 9, 5, 0, 0, 0},
        {0, 9, 8, 0, 0, 0, 0, 6, 0},
        {8, 0, 0, 0, 6, 0, 0, 0, 3},
        {4, 0, 0, 8, 0, 3, 0, 0, 1},
        {7, 0, 0, 0, 2, 0, 0, 0, 6},
        {0, 6, 0, 0, 0, 0, 2, 8, 0},
        {0, 0, 0, 4, 1, 9, 0, 0, 5},
        {0, 0, 0, 0, 8, 0, 0, 7, 9}
    };

    printf("原始数独:\n");
    print_board(board);
    printf("\n");

    if (solve_sudoku(board)) {
        printf("求解成功!\n\n");
        print_board(board);
    } else {
        printf("该数独无解!\n");
    }

    return 0;
}
