import pymysql
c=pymysql.connect(user='root',password='123456',database='anime_mangage_db',charset='utf8mb4')
x=c.cursor()
x.execute("UPDATE t_video_info SET release_date='2000-01-01' WHERE release_date IS NULL")
x.execute("UPDATE t_video_info SET runtime=24 WHERE runtime IS NULL OR runtime=0")
x.execute("UPDATE t_video_info SET budget=1 WHERE budget IS NULL OR budget=0")
x.execute("UPDATE t_video_info SET vote_average=6.5 WHERE vote_average IS NULL OR vote_average=0")
c.commit()
