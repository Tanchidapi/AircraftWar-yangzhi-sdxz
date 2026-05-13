# 飞机大战 - 在线排行榜服务器
# 使用方法:
#   1. 安装依赖: pip install flask
#   2. 启动服务器: python ranking_server.py
#   3. 服务器默认运行在 http://0.0.0.0:5000
#
# API 接口:
#   POST /api/scores          - 提交分数
#   GET  /api/ranking?difficulty=EASY  - 获取排行榜
#   POST /api/battle/rooms                  - 创建对战房间
#   POST /api/battle/rooms/<room_id>/join   - 加入对战房间
#   POST /api/battle/rooms/<room_id>/start  - 房主开始对局
#   POST /api/battle/rooms/<room_id>/scores - 提交对战分数
#   POST /api/battle/rooms/<room_id>/again  - 准备再来一局
#   GET  /api/battle/rooms/<room_id>        - 获取对战房间状态
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
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS battle_rooms (
            room_id TEXT PRIMARY KEY,
            host_name TEXT NOT NULL,
            difficulty TEXT NOT NULL DEFAULT 'EASY',
            status TEXT NOT NULL DEFAULT 'waiting',
            round_no INTEGER NOT NULL DEFAULT 1,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS battle_players (
            room_id TEXT NOT NULL,
            player_name TEXT NOT NULL,
            is_host INTEGER NOT NULL DEFAULT 0,
            ready_next INTEGER NOT NULL DEFAULT 0,
            joined_at TEXT DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY(room_id, player_name)
        )
    ''')
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS battle_scores (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            room_id TEXT NOT NULL,
            round_no INTEGER NOT NULL DEFAULT 1,
            player_name TEXT NOT NULL,
            score INTEGER NOT NULL,
            play_time TEXT NOT NULL,
            difficulty TEXT NOT NULL DEFAULT 'EASY',
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    cursor.execute('PRAGMA table_info(battle_scores)')
    score_columns = [row[1] for row in cursor.fetchall()]
    if 'round_no' not in score_columns:
        cursor.execute('ALTER TABLE battle_scores ADD COLUMN round_no INTEGER NOT NULL DEFAULT 1')
    cursor.execute("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'battle_scores'")
    score_schema = cursor.fetchone()[0]
    if 'UNIQUE(room_id, player_name)' in score_schema:
        cursor.execute('ALTER TABLE battle_scores RENAME TO battle_scores_old')
        cursor.execute('''
            CREATE TABLE battle_scores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                room_id TEXT NOT NULL,
                round_no INTEGER NOT NULL DEFAULT 1,
                player_name TEXT NOT NULL,
                score INTEGER NOT NULL,
                play_time TEXT NOT NULL,
                difficulty TEXT NOT NULL DEFAULT 'EASY',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        cursor.execute('''
            INSERT INTO battle_scores (room_id, round_no, player_name, score, play_time, difficulty, created_at)
            SELECT room_id, round_no, player_name, score, play_time, difficulty, created_at
            FROM battle_scores_old
        ''')
        cursor.execute('DROP TABLE battle_scores_old')
    cursor.execute('''
        CREATE UNIQUE INDEX IF NOT EXISTS idx_battle_scores_room_round_player
        ON battle_scores(room_id, round_no, player_name)
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


def normalize_room_id(room_id):
    return (room_id or '').strip()


def normalize_player_name(player_name):
    return (player_name or 'Player').strip() or 'Player'


def fetch_battle_room(cursor, room_id):
    cursor.execute('SELECT * FROM battle_rooms WHERE room_id = ?', (room_id,))
    room = cursor.fetchone()
    if not room:
        return None

    cursor.execute(
        'SELECT player_name AS playerName, is_host AS isHost, ready_next AS readyNext '
        'FROM battle_players WHERE room_id = ? ORDER BY is_host DESC, joined_at ASC',
        (room_id,)
    )
    players = [dict(row) for row in cursor.fetchall()]

    cursor.execute(
        'SELECT player_name AS playerName, score, play_time AS playTime, difficulty '
        'FROM battle_scores WHERE room_id = ? AND round_no = ? ORDER BY score DESC, created_at ASC',
        (room_id, room['round_no'])
    )
    scores = [dict(row) for row in cursor.fetchall()]

    score_map = {score['playerName']: score for score in scores}
    for player in players:
        score = score_map.get(player['playerName'])
        player['isHost'] = bool(player['isHost'])
        player['readyNext'] = bool(player['readyNext'])
        player['finished'] = score is not None
        player['score'] = score['score'] if score else None
        player['playTime'] = score['playTime'] if score else None
        player['difficulty'] = score['difficulty'] if score else room['difficulty']

    all_finished = len(players) >= 2 and all(player['finished'] for player in players)
    return {
        'roomId': room['room_id'],
        'hostName': room['host_name'],
        'difficulty': room['difficulty'],
        'status': room['status'],
        'roundNo': room['round_no'],
        'allFinished': all_finished,
        'players': players[:2]
    }


@app.route('/api/battle/rooms', methods=['POST'])
def create_battle_room():
    """创建对战房间，创建者为房主。"""
    try:
        data = request.get_json() or {}
        room_id = normalize_room_id(data.get('roomId'))
        player_name = normalize_player_name(data.get('playerName'))
        difficulty = data.get('difficulty', 'EASY')

        if not room_id:
            return jsonify({'error': '房间号不能为空'}), 400
        if difficulty not in ('EASY', 'NORMAL', 'HARD'):
            return jsonify({'error': '难度必须为 EASY/NORMAL/HARD'}), 400

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute('SELECT room_id FROM battle_rooms WHERE room_id = ?', (room_id,))
        if cursor.fetchone():
            conn.close()
            return jsonify({'error': '房间已存在'}), 409

        cursor.execute(
            'INSERT INTO battle_rooms (room_id, host_name, difficulty, status) VALUES (?, ?, ?, ?)',
            (room_id, player_name, difficulty, 'waiting')
        )
        cursor.execute(
            'INSERT INTO battle_players (room_id, player_name, is_host) VALUES (?, ?, 1)',
            (room_id, player_name)
        )
        conn.commit()
        room = fetch_battle_room(cursor, room_id)
        conn.close()

        print(f'[创建对战房间] {room_id} - 房主 {player_name} - {difficulty}')
        return jsonify(room), 201

    except Exception as e:
        print(f'[错误] 创建对战房间失败: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/api/battle/rooms/<room_id>/join', methods=['POST'])
def join_battle_room(room_id):
    """加入已有对战房间。"""
    try:
        data = request.get_json() or {}
        room_id = normalize_room_id(room_id)
        player_name = normalize_player_name(data.get('playerName'))
        if not room_id:
            return jsonify({'error': '房间号不能为空'}), 400

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute('SELECT status FROM battle_rooms WHERE room_id = ?', (room_id,))
        room = cursor.fetchone()
        if not room:
            conn.close()
            return jsonify({'error': '房间不存在'}), 404

        cursor.execute('SELECT COUNT(*) FROM battle_players WHERE room_id = ?', (room_id,))
        player_count = cursor.fetchone()[0]
        cursor.execute(
            'SELECT player_name FROM battle_players WHERE room_id = ? AND player_name = ?',
            (room_id, player_name)
        )
        exists = cursor.fetchone() is not None
        if player_count >= 2 and not exists:
            conn.close()
            return jsonify({'error': '房间已满'}), 409

        if not exists:
            cursor.execute(
                'INSERT INTO battle_players (room_id, player_name, is_host) VALUES (?, ?, 0)',
                (room_id, player_name)
            )
        conn.commit()
        battle_room = fetch_battle_room(cursor, room_id)
        conn.close()

        print(f'[加入对战房间] {room_id} - {player_name}')
        return jsonify(battle_room), 200

    except Exception as e:
        print(f'[错误] 加入对战房间失败: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/api/battle/rooms/<room_id>/start', methods=['POST'])
def start_battle_room(room_id):
    """房主开始对局。"""
    try:
        data = request.get_json() or {}
        room_id = normalize_room_id(room_id)
        player_name = normalize_player_name(data.get('playerName'))

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute('SELECT host_name FROM battle_rooms WHERE room_id = ?', (room_id,))
        room = cursor.fetchone()
        if not room:
            conn.close()
            return jsonify({'error': '房间不存在'}), 404
        if room['host_name'] != player_name:
            conn.close()
            return jsonify({'error': '只有房主可以开始游戏'}), 403

        cursor.execute('SELECT COUNT(*) FROM battle_players WHERE room_id = ?', (room_id,))
        if cursor.fetchone()[0] < 2:
            conn.close()
            return jsonify({'error': '需要两名玩家就绪后才能开始'}), 409

        cursor.execute(
            "UPDATE battle_rooms SET status = 'playing', updated_at = CURRENT_TIMESTAMP WHERE room_id = ?",
            (room_id,)
        )
        cursor.execute('UPDATE battle_players SET ready_next = 0 WHERE room_id = ?', (room_id,))
        conn.commit()
        battle_room = fetch_battle_room(cursor, room_id)
        conn.close()

        print(f'[开始对战] {room_id} - 第 {battle_room["roundNo"]} 局')
        return jsonify(battle_room), 200

    except Exception as e:
        print(f'[错误] 开始对战失败: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/api/battle/rooms/<room_id>/scores', methods=['POST'])
def submit_battle_score(room_id):
    """提交当前局对战分数。"""
    try:
        data = request.get_json() or {}
        room_id = normalize_room_id(room_id)
        player_name = normalize_player_name(data.get('playerName'))
        score = data.get('score', 0)
        play_time = data.get('playTime', datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
        difficulty = data.get('difficulty', 'EASY')
        round_no = data.get('roundNo')

        if not room_id:
            return jsonify({'error': '房间号不能为空'}), 400
        if not isinstance(score, int) or score < 0:
            return jsonify({'error': '分数必须为非负整数'}), 400

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute('SELECT round_no, difficulty FROM battle_rooms WHERE room_id = ?', (room_id,))
        room = cursor.fetchone()
        if not room:
            conn.close()
            return jsonify({'error': '房间不存在'}), 404
        current_round = room['round_no']
        if round_no is not None and int(round_no) != current_round:
            conn.close()
            return jsonify({'error': '对局轮次已过期'}), 409

        cursor.execute(
            'DELETE FROM battle_scores WHERE room_id = ? AND round_no = ? AND player_name = ?',
            (room_id, current_round, player_name)
        )
        cursor.execute(
            'INSERT INTO battle_scores (room_id, round_no, player_name, score, play_time, difficulty) '
            'VALUES (?, ?, ?, ?, ?, ?)',
            (room_id, current_round, player_name, score, play_time, difficulty or room['difficulty'])
        )
        cursor.execute(
            'SELECT COUNT(*) FROM battle_scores WHERE room_id = ? AND round_no = ?',
            (room_id, current_round)
        )
        if cursor.fetchone()[0] >= 2:
            cursor.execute(
                "UPDATE battle_rooms SET status = 'finished', updated_at = CURRENT_TIMESTAMP WHERE room_id = ?",
                (room_id,)
            )
        conn.commit()
        battle_room = fetch_battle_room(cursor, room_id)
        conn.close()

        print(f'[对战提交] 房间 {room_id} - 第 {current_round} 局 - {player_name} - {score}分')
        return jsonify(battle_room), 201

    except Exception as e:
        print(f'[错误] 提交对战分数失败: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/api/battle/rooms/<room_id>/again', methods=['POST'])
def ready_battle_again(room_id):
    """玩家准备再来一局，两人都准备后房间回到 waiting，等待房主开始。"""
    try:
        data = request.get_json() or {}
        room_id = normalize_room_id(room_id)
        player_name = normalize_player_name(data.get('playerName'))

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute('SELECT round_no FROM battle_rooms WHERE room_id = ?', (room_id,))
        room = cursor.fetchone()
        if not room:
            conn.close()
            return jsonify({'error': '房间不存在'}), 404

        cursor.execute(
            'UPDATE battle_players SET ready_next = 1 WHERE room_id = ? AND player_name = ?',
            (room_id, player_name)
        )
        cursor.execute('SELECT COUNT(*) FROM battle_players WHERE room_id = ?', (room_id,))
        player_count = cursor.fetchone()[0]
        cursor.execute(
            'SELECT COUNT(*) FROM battle_players WHERE room_id = ? AND ready_next = 1',
            (room_id,)
        )
        ready_count = cursor.fetchone()[0]
        if player_count >= 2 and ready_count >= 2:
            cursor.execute(
                "UPDATE battle_rooms SET status = 'waiting', round_no = round_no + 1, "
                "updated_at = CURRENT_TIMESTAMP WHERE room_id = ?",
                (room_id,)
            )
            cursor.execute('UPDATE battle_players SET ready_next = 0 WHERE room_id = ?', (room_id,))
        conn.commit()
        battle_room = fetch_battle_room(cursor, room_id)
        conn.close()

        print(f'[再来一局] {room_id} - {player_name}')
        return jsonify(battle_room), 200

    except Exception as e:
        print(f'[错误] 再来一局失败: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/api/battle/rooms/<room_id>/leave', methods=['POST'])
def leave_battle_room(room_id):
    """退出房间。"""
    try:
        data = request.get_json() or {}
        room_id = normalize_room_id(room_id)
        player_name = normalize_player_name(data.get('playerName'))

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute(
            'DELETE FROM battle_players WHERE room_id = ? AND player_name = ?',
            (room_id, player_name)
        )
        cursor.execute('SELECT COUNT(*) FROM battle_players WHERE room_id = ?', (room_id,))
        if cursor.fetchone()[0] == 0:
            cursor.execute('DELETE FROM battle_scores WHERE room_id = ?', (room_id,))
            cursor.execute('DELETE FROM battle_rooms WHERE room_id = ?', (room_id,))
            room = None
        else:
            cursor.execute('SELECT host_name FROM battle_rooms WHERE room_id = ?', (room_id,))
            existing_room = cursor.fetchone()
            if existing_room and existing_room['host_name'] == player_name:
                cursor.execute(
                    'SELECT player_name FROM battle_players WHERE room_id = ? ORDER BY joined_at ASC LIMIT 1',
                    (room_id,)
                )
                new_host = cursor.fetchone()['player_name']
                cursor.execute(
                    "UPDATE battle_rooms SET host_name = ?, status = 'waiting' WHERE room_id = ?",
                    (new_host, room_id)
                )
                cursor.execute('UPDATE battle_players SET is_host = 0 WHERE room_id = ?', (room_id,))
                cursor.execute(
                    'UPDATE battle_players SET is_host = 1 WHERE room_id = ? AND player_name = ?',
                    (room_id, new_host)
                )
            room = fetch_battle_room(cursor, room_id)
        conn.commit()
        conn.close()

        print(f'[退出房间] {room_id} - {player_name}')
        return jsonify(room or {'success': True}), 200

    except Exception as e:
        print(f'[错误] 退出房间失败: {e}')
        return jsonify({'error': str(e)}), 500


@app.route('/api/battle/rooms/<room_id>', methods=['GET'])
def get_battle_room(room_id):
    """获取对战房间状态。"""
    try:
        room_id = normalize_room_id(room_id)
        if not room_id:
            return jsonify({'error': '房间号不能为空'}), 400

        conn = get_db()
        cursor = conn.cursor()
        room = fetch_battle_room(cursor, room_id)
        conn.close()
        if not room:
            return jsonify({'error': '房间不存在'}), 404

        print(f'[查询对战房间] {room_id} - {len(room["players"])} 位玩家 - {room["status"]}')
        return jsonify(room), 200

    except Exception as e:
        print(f'[错误] 获取对战房间失败: {e}')
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
