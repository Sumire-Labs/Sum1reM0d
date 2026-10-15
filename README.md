# Sum1reM0d

Sumire Labs の不条理なルールをMinecraftへ追加するMod

## ビルドとリリース

push・プルリクエスト時にGitHub Actionsで全対応対象をビルド・テストします。
テスト用JARはActionsのBuild実行ページにあるArtifactsから取得できます。

Gitタグをpushすると、全対象のビルド成功後にGitHub Releaseを自動作成し、
Minecraft版・ローダーごとのJARを添付します。リリースノートも自動生成します。
例えば `git tag 1.0.1` → `git push origin 1.0.1` で公開します。
利用者は自分のMinecraft版・ローダーに合うJARを1個選んで導入してください。
Forgixによる統合は現在無効です。
