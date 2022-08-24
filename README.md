# Pointlionnaire
A project for a bot in twitch chat to ask questions and find a winner.

This project idea comes from Donut [twitch.tv/adonutirl](https://www.twitch.tv/adonutirl)

### General Info
* This Bot should be a Moderator on your channel, so it does not get blocked for spam or shadow-banned even.
* You need a token from a twitch account so the bot can use it.
* Keep in mind that the compiled executable can do anything on your PC. The code is openly accessible and can be reviewed in this repository, yet I will take no blame if something goes wrong.
* As of Version 1.0.0, it is not possible to set questions/redeems via UI (this feature is planned for Version 2.0.0). There are two JSON-Files that hold the information. You need to manually edit them. More information is below.

### Setup
Before executing the program, you need a folder "data" on the same level as the executable with following files and their contents:
* twitchBotConfig.properties (replace explanations in <> with the needed data)
    * channel=\<channel name>
    * only_mods=\<true if only mods should be able to use the bot>
    * command_prefix=\<prefix for commands>
    * leave_emote=\<twitch emote that appears when the bot leaves the chat>
    * arrive_emote=\<twitch emote that appears when the bot connects to chat>
    * explanation_emote=\<twitch emote that appears when the bot explains>
    * blacklisted_users=\<a list of blacklisted users, separated by ",">
    * blacklist_emote=\<twitch emote that appears when the bot messages a blacklisted user>
    * no_question_pending_text=\<text that gets displayed when there is no question pending>
    * maximum_rolls=\<amount of rerolls the winner is allowed to use when redeeming a prize>
    * no_more_rerolls_text=\<text that gets displayed when no more rerolls are open for the winner>
    * gg_emote=\<twitch emote that appears when something gets celebrated>
    * amount_questions=\<parameter to set the amount of questions that will be asked. This is an integer and should be positive>
    * total_interval_duration=\<parameter to set the total interval time in which the questions will be asked. This is a double and is interpreted as minutes>
    * answer_duration=\<parameter to set the time each question can get answered. This is a double and is interpreted as minutes>
    * attention_emote=\<twitch emote that is used to announce something>
    * time_up_emote=\<twitch emote that is used to announce that the time is over>
    * points_for_top_3=\<a list to set the parameters how many points each player gets for each question. This is a list of integers and separated by ",". First number is for the first player, e.g. "3,2,1">
    * game_up_emote=\<twitch emote that is used when the whole game is over>
    * no_winner_emote=\<twitch emote that is used when there is no winner at the end (that means no one got one answer right)>
    * tie_emote=\<twitch emote that is used when a tie gets announced>
    * tiebreaker_answer_duration=\<parameter to set the time each tie breaker question can get answered. This is a double and is interpreted as minutes>
    * max_amount_tries=\<maximum amount of tries each user gets for each question>
    * something_went_wrong_emote=\<twitch emote that is used to announce that something went wrong>
* questions.json:
  * This file holds the information for all questions. They are structured in JSON-format. The Objects in the array need following structure:
    ```json
    [
      {
        "questionText": "This is test question 1",
        "answer": "Test answer one",
        "isLast2Questions": false,
        "isTieBreakerQuestion": false
      },
      {
        "questionText": "This is test question 2",
        "answer": "Test answer two",
        "isLast2Questions": false,
        "isTieBreakerQuestion": false
      }
    ]
    ```
  Keep in mind that the questions are JSON-Objects bracketed in ``{}`` and those objects are inside an array bracketed in ``[]`` <br>
  * Short explanation for each field:
    * questionText: Question's text, String. 
    * answer: Question's answer, String.
    * isLast2Questions: Flag if a questions only gets asked as last two questions, Boolean. Last two questions bring doubled points.
    * isTieBreakerQuestion: Flag if a question only gets asked as tie breaker questions, Boolean. Tie breaker questions should be easy to answer.
  * For the program to work, you need following amount of questions:
    * At least <amountQuestions> total questions who are no tiebreaker questions. 
    * At least <amountQuestions - 2> questions which are neither tiebreaker nor last 2 questions. 
    * At least 2 questions which are last 2 questions and no tiebreaker. 
    * At least 1 tiebreaker question.
* redeems.json:
  * This file holds the information for all redeems. They are structured in JSON-format. The Strings are bracketed in an JSON-Array:
  ```json
  [
    "Redeem 1",
    "Redeem 2",
    "Redeem 3"
  ]
  ```
  
* twitchtoken.txt
    * The only content: twitch bot account token


### Info for certain properties:
  * User lists: When you use a list of users (e.g. for the blacklist feature), you can write down their name in the properties file. Just keep in mind that they can easily change their name, which means it is better to use their IDs instead. For that the bot will issue a warning that contains the user's ID. Read the log-file to gather that information.