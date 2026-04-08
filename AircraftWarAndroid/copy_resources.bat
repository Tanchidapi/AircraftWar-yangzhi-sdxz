@echo off
chcp 65001 >nul
echo ========================================
echo  飞机大战 Android 资源复制脚本
echo ========================================
echo.

set SRC_IMG=..\AircraftWar-base\AircraftWar-base\src\images
set SRC_SND=..\AircraftWar-base\AircraftWar-base\src\videos
set DST_IMG=app\src\main\res\drawable
set DST_SND=app\src\main\res\raw

echo 正在复制图片资源...
copy "%SRC_IMG%\bg.jpg" "%DST_IMG%\bg.jpg"
copy "%SRC_IMG%\bg2.jpg" "%DST_IMG%\bg2.jpg"
copy "%SRC_IMG%\bg3.jpg" "%DST_IMG%\bg3.jpg"
copy "%SRC_IMG%\hero.png" "%DST_IMG%\hero.png"
copy "%SRC_IMG%\mob.png" "%DST_IMG%\mob.png"
copy "%SRC_IMG%\elite.png" "%DST_IMG%\elite.png"
copy "%SRC_IMG%\elitePlus.png" "%DST_IMG%\eliteplus.png"
copy "%SRC_IMG%\boss.png" "%DST_IMG%\boss.png"
copy "%SRC_IMG%\bullet_hero.png" "%DST_IMG%\bullet_hero.png"
copy "%SRC_IMG%\bullet_enemy.png" "%DST_IMG%\bullet_enemy.png"
copy "%SRC_IMG%\prop_blood.png" "%DST_IMG%\prop_blood.png"
copy "%SRC_IMG%\prop_bomb.png" "%DST_IMG%\prop_bomb.png"
copy "%SRC_IMG%\prop_bullet.png" "%DST_IMG%\prop_bullet.png"
copy "%SRC_IMG%\prop_bulletPlus.png" "%DST_IMG%\prop_bulletplus.png"

echo.
echo 正在复制音效资源...
echo 注意: Android raw 目录不支持 .wav 格式的大文件
echo 建议将 .wav 转换为 .ogg 格式以减小APK体积
echo 当前直接复制 .wav 文件...
copy "%SRC_SND%\bgm.wav" "%DST_SND%\bgm.wav"
copy "%SRC_SND%\bgm_boss.wav" "%DST_SND%\bgm_boss.wav"
copy "%SRC_SND%\bomb_explosion.wav" "%DST_SND%\bomb_explosion.wav"
copy "%SRC_SND%\bullet.wav" "%DST_SND%\bullet.wav"
copy "%SRC_SND%\bullet_hit.wav" "%DST_SND%\bullet_hit.wav"
copy "%SRC_SND%\game_over.wav" "%DST_SND%\game_over.wav"
copy "%SRC_SND%\get_supply.wav" "%DST_SND%\get_supply.wav"

echo.
echo ========================================
echo  资源复制完成！
echo  请用 Android Studio 打开 AircraftWarAndroid 项目
echo ========================================
pause
