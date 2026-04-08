# 飞机大战 - 在线排行榜服务器
# 使用方法:
#   1. 安装依赖: pip install flask
#   2. 启动服务器: python ranking_server.py
#   3. 服务器默认运行在 http://0.0.0.0:5000
#
# API 接口:
#   POST /api/scores          - 提交分数
#   GET  /api/ranking?difficulty=EASY  - 获取排行榜
#   GET  /api/health           - 健康检查

import sqlite3
import json
import os
from datetime import datetime
from flask import Flask, request, jsonify

app = Flask(__name__)

# 数据库文件路径（与脚本同目录）
DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'ranking.db')


def get_db():
    """获取数据库连接"""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    """初始化数据库表"""
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS scores (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            score INTEGER NOT NULL,
            play_time TEXT NOT NULL,
            difficulty TEXT NOT NULL DEFAULT 'EASY',
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    conn.commit()

    # 如果表为空，插入一些初始数据
    cursor.execute('SELECT COUNT(*) FROM scores')
    count = cursor.fetchone()[0]
    if count == 0:
        initial_data = [
            ('AcePlayer', 2580, '2026-04-01 10:30:00', 'EASY'),
            ('SkyKing', 1920, '2026-04-02 14:20:00', 'EASY'),
            ('StarPilot', 1650, '2026-04-03 09:15:00', 'EASY'),
            ('Phoenix', 1200, '2026-04-04 16:45:00', 'EASY'),
            ('Thunder', 980, '2026-04-05 11:00:00', 'EASY'),
            ('AcePlayer', 3200, '2026-04-01 11:00:00', 'NORMAL'),
            ('DragonFly', 2800, '2026-04-02 15:30:00', 'NORMAL'),
            ('SkyKing', 2100, '2026-04-03 10:00:00', 'NORMAL'),
            ('IronWing', 1750, '2026-04-04 17:20:00', 'NORMAL'),
            ('Blaze', 1400, '2026-04-05 12:10:00', 'NORMAL'),
            ('AcePlayer', 4500, '2026-04-01 12:00:00', 'HARD'),
            ('DragonFly', 3800, '2026-04-02 16:00:00', 'HARD'),
            ('Viper', 3100, '2026-04-03 11:30:00', 'HARD'),
            ('SkyKing', 2600, '2026-04-04 18:00:00', 'HARD'),
            ('StormRider', 2000, '2026-04-05 13:00:00', 'HARD'),
        ]
        cursor.executemany(
            'INSERT INTO scores (name, score, play_time, difficulty) VALUES (?, ?, ?, ?)',
            initial_data
        )
        conn.commit()
        print(f'[初始化] 已插入 {len(initial_data)} 条初始排行榜数据')

    conn.close()


@app.route('/api/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({'status': 'ok', 'message': '飞机大战排行榜服务器运行中'})


@app.route('/api/scores', methods=['POST'])
def submit_score():
    """提交分数"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请求体为空'}), 400

        name = data.get('name', 'Player')
        score = data.get('score', 0)
        play_time = data.get('playTime', datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
        difficulty = data.get('difficulty', 'EASY')

        # 参数校验
        if not isinstance(score, int) or score < 0:
            return jsonify({'error': '分数必须为非负整数'}), 400
        if difficulty not in ('EASY', 'NORMAL', 'HARD'):
            return jsonify({'error': '难度必须为 EASY/NORMAL/HARD'}), 400

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute(
            'INSERT INTO scores (name, score, play_time, difficulty) VALUES (?, ?, ?, ?)',
            (name, score, play_time, difficulty)
        )
        conn.commit()
        new_id = cursor.lastrowid
        conn.close()

        print(f'[提交分数] {name} - {score}分 - {difficulty}')
        return jsonify({'success': True, 'id': new_id}), 201

    except Exception as e:
        print(f'[错误] 提交分数失败: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/api/ranking', methods=['GET'])
def get_ranking():
    """获取排行榜（按分数降序，最多返回20条）"""
    try:
        difficulty = request.args.get('difficulty', 'EASY')
        limit = request.args.get('limit', 20, type=int)
        limit = min(limit, 50)  # 最多50条

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute(
            'SELECT name, score, play_time AS playTime, difficulty FROM scores '
            'WHERE difficulty = ? ORDER BY score DESC LIMIT ?',
            (difficulty, limit)
        )
        rows = cursor.fetchall()
        conn.close()

        # 转换为字典列表
        result = [dict(row) for row in rows]
        print(f'[查询排行榜] {difficulty} - 返回 {len(result)} 条记录')
        return jsonify(result), 200

    except Exception as e:
        print(f'[错误] 获取排行榜失败: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/api/scores/<int:score_id>', methods=['DELETE'])
def delete_score(score_id):
    """删除指定分数记录"""
    try:
        conn = get_db()
        cursor = conn.cursor()
        cursor.execute('DELETE FROM scores WHERE id = ?', (score_id,))
        conn.commit()
        deleted = cursor.rowcount
        conn.close()

        if deleted > 0:
            return jsonify({'success': True}), 200
        else:
            return jsonify({'error': '记录不存在'}), 404

    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    print('=' * 50)
    print('  飞机大战 - 在线排行榜服务器')
    print('=' * 50)
    init_db()
    print(f'[启动] 数据库路径: {DB_PATH}')
    print(f'[启动] 服务器地址: http://0.0.0.0:5000')
    print(f'[启动] 健康检查: http://localhost:5000/api/health')
    print(f'[启动] 排行榜API: http://localhost:5000/api/ranking?difficulty=EASY')
    print('=' * 50)
    # host='0.0.0.0' 允许局域网内其他设备（包括模拟器）访问
    app.run(host='0.0.0.0', port=5000, debug=True)
