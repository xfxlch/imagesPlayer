push local project to github
1.��github���½�һ����������Ŀͬ���Ĳֿ�
2.����һ�£���Ȩ�Ƿ�ɹ���ssh -T git@github.com��,���ɹ��Ļ���Ҫ�������ssh key
3.�ϴ�������Ŀ��github֮ǰҪ����username��email����Ϊgithubÿ��commit�����¼����
 ��$ git config --global user.name "your name"����$ git config --global user.email "your_email@youremail.com"��
4.cd ��������ĿĿ¼��ִ����� $ git remote add origin git@github.com:yourName/yourRepo.git��
5.�ڱ��زֿ������һЩ�ļ�������README��$ git add README$ git commit -m "first commit"��
6.�ϴ���github����$ git push origin master��
   git push����Ὣ���زֿ����͵�Զ�̷�������
   git pull�������෴��
7.ִ�н����
$ git push origin master
Counting objects: 19, done.
Delta compression using up to 4 threads.
Compressing objects: 100% (11/11), done.
Writing objects: 100% (18/18), 7.83 MiB | 35.00 KiB/s, done.
Total 18 (delta 0), reused 0 (delta 0)
To git@github.com:xfxlch/imagesPlayer.git
   93243d9..9f168f3  master -> master

�����ϴ��ɹ���