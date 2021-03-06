Портативні репозиторії документів
==============================

Базове використання
-----------
Портативна версія DocFetcher'а по суті дозволяє вам переносити (та навіть розповсюджувати) повністю індексований та повністю здатний для пошуку репозиторій документів. Якщо у вас ще немає портативної версії, її можна завантажити з [веб-сторінки проекту](http://docfetcher.sourceforge.net).

Портативна версія не вимагає будь-якої інсталяції; просто витягніть вміст архіву у бажану вам теку. Ви можете далі запускати DocFetcher через відповідний пускач для вашої операційної системи: `DocFetcher.exe` на Windows, `DocFetcher.sh` на Linux та `DocFetcher` application bundle на Mac OS&nbsp;X. Єдиною вимогою є наявність інстальованого виконуваного середовища Java з версією 1.6 або новіше на цьому комп'ютері.

<u>Відносні шляхи</u>: Важливо звернути увагу на те, щоб усі індекси обов'язково створювалися з увімкненою уставою *відносні шляхи*. Без цього DocFetcher буде зберігати *абсолютні* посилання на ваші файли, а тому ви зможете тільки переміщати DocFetcher та його індекси, а не ваші файли &mdash; принаймні не без розривання посилань. Ось приклад, що ілюструє це:

* Відносний шлях: `..\..\my-files\some-document.txt`
* Абсолютний шлях: `C:\my-files\some-document.txt`

Відносний шлях каже DocFetcher'у, що може знайти `some-document.txt`, перейшовши на два рівні від поточно розміщення та спустившись у теку `my-files`. Абсолютний шлях, з іншого боку, є фіксованим посиланням та незалежним від поточного розміщення DocFetcher'а, а тому ви не можете перемістити `some-document.txt` без розривання цього посилання (а значить DocFetcher не зможе знайти цей файл).

Зауважте, що DocFetcher може лише *пробувати* зберігати відносні шляхи: очевидно, він не зможе зробити це, якщо ви покладете  DocFetcher та ваші файли на різні логічні диски, наприклад, DocFetcher у `D:\DocFetcher`, а ваші файли у `E:\my-files`.

Поради щодо зручності використання
--------------

* ***Архівування на CD-ROM***: Просто логічно, але все таки: якщо ви покладете DocFetcher на CD-ROM, ви не будете у змозі зберігати зміни в уподобаннях або індексах, а тому пам'ятайте все належно налаштовувати перед записуванням на CD-ROM. Також, ви можливо схочете включити туди інсталятор робочого середовища Java.
* ***Різні заголовки програми***: Для перерозподілу вашого портативного репозиторію документів або для менш заплутаної роботи з одночасно кількома примірниками DocFetcher ви можете дати кожному примірнику DocFetcher'а різний заголовок програми для кожного його вікна. Для цього відкрийте `Просунуті устави` у діалогу уподобань та модифікуйте уставу `AppName`.

Застереження
--------

* ***Не чіпайте теку `indexes`***: Ви можете, але це не потрібно, класти ваші файли прямо у теку DocFetcher'а. Якщо ви це робите, не чіпайте теку `indexes`, оскільки все, що ви покладете в неї, може бути видалено!
* ***Несумісності імен файлів***: Остерігайтеся несумісностей імен файлів між різними операційними системами. Наприклад, на Linux імена файлів можуть містити такі символи, як ":" або "|", але на Windows це не дозволяється. У результаті, ви можете переміщувати репозиторій документів з Linux на Windows або навпаки, якщо він не містить документи з несумісними іменами. О, і спеціальні символи, такі як німецькі умляути - це зовсім інша справа...