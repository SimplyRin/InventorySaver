# InventorySaver
プレイヤーのインベントリ(アーマー、アイテム)、経験値レベルを指定した時間おきに保存し、後から確認したり読み込んだりできる物。

# Commands

|コマンド|内容|権限|
|---|---|---|
|`/viewinv <保存するプレイヤー名> (オプション: ページ数)`|指定ユーザーの保存ファイルを確認|inventorysaver.admin|
|`/loadinv <復元対象プレイヤー名> <ファイルパス, ユーザー名/保存ファイル.yml>`|指定したユーザーのインベントリを復元|inventorysaver.admin|
|`/saveinv`|オンラインプレイヤーのインベントリを保存|inventorysaver.admin|

# douga

https://user-images.githubusercontent.com/26184969/169355227-ec6dbddb-f637-4d81-abe7-ef87297740ff.mp4

過去データはサーバー起動時自動的に日付ごとに ZIP 化してくれます

![2022-05-20-01_53_42_mstsc](https://user-images.githubusercontent.com/26184969/169355557-7795db8d-c7db-44f3-bf84-70d035049673.png)
