push local project to github
1.在github上新建一个跟本地项目同名的仓库
2.测试一下，授权是否成功【ssh -T git@github.com】,不成功的话，要重新添加ssh key
3.上传本地项目到github之前要设置username和email，因为github每次commit都会记录他们
 【$ git config --global user.name "your name"】【$ git config --global user.email "your_email@youremail.com"】
4.cd 到本地项目目录，执行命令【 $ git remote add origin git@github.com:yourName/yourRepo.git】
5.在本地仓库里添加一些文件，比如README【$ git add README$ git commit -m "first commit"】
6.上传到github：【$ git push origin master】
   git push命令会将本地仓库推送到远程服务器。
   git pull命令则相反。
7.执行结果：
$ git push origin master
Counting objects: 19, done.
Delta compression using up to 4 threads.
Compressing objects: 100% (11/11), done.
Writing objects: 100% (18/18), 7.83 MiB | 35.00 KiB/s, done.
Total 18 (delta 0), reused 0 (delta 0)
To git@github.com:xfxlch/imagesPlayer.git
   93243d9..9f168f3  master -> master

表明上传成功！