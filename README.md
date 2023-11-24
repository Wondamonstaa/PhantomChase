# CS 441 Project 3 Fall 2023
## Kiryl Baravikou
### UIN: 656339218
### NetID: kbara5

Repo for the PhantomChase Project 3 for CS 441 Fall 2023

---
AWS EMR Deployment YouTube link: EDIT
---

## How to Run the Project:
1) Start by downloading the project repository from Git. Please, note, that the git repository contains the folder TO_USE. The following folder has all the essential resources I have been using to successfully run the project.
2) Navigate to the PhantomChase directory: use your terminal or command prompt to navigate to the "PhantomChase" directory within the downloaded project folder.
3) Before your run the project, please, make sure that your current setting satisfy the requirements: a) Scala version: 2.13.10; b) JDK version: 1.8; c) SBT version: 1.9.6; d) Apache Spark version: 3.5.0; e) Akka HTTP 10.5.0;
4) After all the prerequisites are met, please, go to File -> Project Structure -> Modules -> Add -> JARs or Directories -> select TO_USE folder -> netmodelsim.jar -> select the uploaded module -> set scope to 'Compile' -> click 'Apply' -> click 'OK'. By doing so I ensure that the essential injected binary files will not be missing. The following .jar file allows me to get access to the methods implemented in the original project created by Dr. Mark. Specified setting in the build.sbt allow to mix multiple Scala versions without any errors.
5) In your terminal, run the following command to clean, compile, and execute the project: `sbt clean compile run`. The following command successfully launched the GameLogic class of the program which contains the essential methods for program execution of the written methods within other objects, and server its primary purpose as the storage of the game logic and drivers.
6) To run tests and ensure the code's functionality, use the following command: `sbt clean test`. The tests written are located within src/test/scala/RandomWalkerTest path directory. All the tests on my end ran successfully among with tests written by Dr. Mark.
7) If you need to create a .jar file for your project, execute the following command: `sbt clean assembly`. The resulting jar will be placed in __NetGameSim/target/scala-3.2.2/game.jar__. Alternatively, a user can use the manual approach to create a .jar file of the project: File -> Project Structure -> Artifacts -> Add -> JAR -> From modules with dependencies -> Specify the main class of the project -> click 'OK' -> go to Build -> Build Artifacts -> in the “Build Artifacts” dialog window, click on “Build” to generate your JAR file. The generated .jar file will be located in the specified output directory.
8) If you prefer using IntelliJ for development: a) Import the project into IntelliJ IDE. b) Build the project. c) Create a "configuration" for the GameLogic.scala and GraphHolder.scala files. d) Set the configuration's arguments to be the input and output folders, separated by a space. NB: to run the project properly, a user, i.e. you, should specify the input/output paths in the application.conf file. The current configurations are set to default values which you may have to change to run the project successfully.
9) Ensure that your local input/output folder has the necessary permissions to allow the program to read from and write to it. You can check and adjust folder permissions by right-clicking on the project directory, selecting "Properties," and navigating to the "Security" tab. Make sure that the group "Users" has both "Read" and "Write" permissions.
10) Before running the program, ensure that Hadoop is running on your machine. The program may depend on Hadoop, so it's essential to have it set up and running correctly.
---

## Requirements:

The goal of the project is to expand the experience with solving distributed computational problems using cloud computing technologies. Next, design and implement a variant of the graph game called 'Policeman and Thief', deploy it on the cloud as a microservice and enable clients to play it using HTTP requests. The following core functionalities must be met:

1. Design: 
a) Design the P/T game, explain the design and architecture, and then implement and run it on the big generated data graphs using predefined configuration parameters for NetGraphSim; 
b) Design and implementation of the game must be done in cloud using microservices such that the clients can play this game over the Internet; 
c) Players submit their moves/queries using 'curl', 'Postman' or using HTTP client request functionality in IntelliJ and the game server will accept these requests, and produce responses to the clients.
2. RESTful services: implement a RESTful service for retrieving log messages using one of the popular frameworks: Play or Finch/Finagle or Akka HTTP or Scalatra. 
3. Functionality: 
a) Players must be able to make moves by taking turns; 
b) Each of the players must be able to query the perturbed graph to obtain information about their own and the opponent's location nodes and the adjacent nodes; 
c) Each of the nodes obtained using the query comes with some confidence score that the node or the edges that lead to it were not perturbed; 
d) Players can also query the PG to determine how far they are from the nearest node with the valuable data. 5) If a player makes a move in PG that cannot be performed in OG s/he loses the game in addition to the basic game rules.
4. Rules: 
a) The game starts by placing Policeman and Thief randomly at some nodes in the original graph and their counterparts at perturbed graph; 
b) If Thief is placed at the node with the valuable data then Thief wins by default and the game restarts; 
c) If Policeman catches the Thief before he/she enters the node where the valuableData attribute is 'true', then the Policeman wins and the game restarts; 
d) If the Thief enters the node with the valuableData before the Policeman catches him/her, the Thief wins and the game restarts; 
e) The first player with no available moves left loses the game;
f) If a player makes a move in perturbed graph that cannot be performed in original graph he/she loses the game in addition to the basic game rules. 
5. Evaluation: 
a) Assess the goodness of your algorithm by generating the confidence score for each node; 
b) Additionally, implement the algorithm for finding the shortest possbile path leading to the node where the valuableData attribute holds the 'true' value.

Other Requirements:
1) The output files should be in the format of .csv, .yaml or any other human-readible format.
2) 5 or more scalatests should be implemented.
3) Logging used for all programs.
4) Configurable input and output paths for the Apache Spark programs.
5) Compileable through 'sbt clean compile run'
7) Deployed on AWS EC2 IaaS demonstrating all the steps of deployment.
8) Video explanation of the deployment recorded.
9) Game logic must be implemented.
10) Sophisticated documentation with clarifications of the chosen design rationale.

---

## Technical Design

This section will provide a detailed description of each of the classes that I implemented in this project, as well as the rationale for using one or another class when planning and developing the design of an algorithm for calculating similarities in the generated graphs:


1. GraphHolder:

GraphHolder, located under src/main/scala/Game, this time serves as the entry point of the program. The following object obtains the configurations, specified in the application.conf file, using the ConfigFactory class, and loads the essential resources for the game to play. The entry point of the object is the 'main' method, which was left empty to simplify the program flow and make the objects accessible by the GameLogic and other objects easily throughout the project. After getting the configurations using the globalConfig value of Config type, using the load() method I load the original and perturbed graphs located under the specified paths, which later will be accessed by the GameLogic object to successfully run the server. Below you can see an example of a generated graph containing 8000 nodes.

# Sample output:

_________________

![WhatsApp Image 2023-09-11 at 10 02 36 PM](https://github.com/Wondamonstaa/NetGameSim_Project1/assets/113752537/208bc260-e8e5-4099-a62d-7e72a49561a1)


_________________

2. GameLogic:

Located under the Game folder, the following object serves its primary role as the core of the game by implementing the logic for the two actors using akka HTTP library: Thief and Policeman. The entry point of the program is the main() method. Inside the following method I initialize the SparkSession using builder() method to access the wide range of tools offered by the Spark library for further usage. Next, I initialized the ActorSystem, which is the home to a hierarchy of Actors. It is created using apply() method from a Behavior object that describes the root Actor of this hierarchy and which will create all other Actors beneath it. Following the ActorSystem, the essential ExecutionContext was created which allows to execute program logic asynchronously. The route of the game with the specified path contains the basic rules and conditions that must be met for normal program flow. The two basic methods used to effectively play the game are defined and implemented within the route: PUT and GET. PUT allows each client to send a request to the server using the 'curl' command, which results in a player making a move to the selected node from the list of adjacent nodes to the current node, while the GET method allows to query the perturbed graph in order to obtain the essential statistics about our own and opponent's adjacent nodes, the shortest path to the node with the valuableData attribute, the confidence score for the current node, and the current list of moves that players have taken so far. Using the GET method, users can obtain the information in the form of a Yaml file, and, additionally, using the Apache Spark library by representing the data in the form of Datasets of String type for further analysis. Besides that, the GameLogic object contains a variety of case classes used to store and access the data collected by the server, and obtained by users using GET command. The responsible case class for providing the data to users during the query is the GetMoves(playerId: String, replyTo: ActorRef[Moves]) case class, which sends the reply to the specified actor under user request. Additioanlly, the RootJsonFormat was used to simplify the game flow and improve the data readibility for users. The GameLogic object contains a nested Puppets object, that stores the current location of both players as well as initializes the starting position of each player using the findStartingPositions() method. Puppets object also specifies the behavior of each actor by implementing the apply() and applyBehavior() methods which define the initial behavior for the actors, and the methods to invoke when reacting to the user 'curl' commands. Finally, two objects representing actors of the game, ThiefActor and PolicemanActor, contain the basic behavior of each actor.

# Sample output:

_________________

<img width="1320" alt="Screenshot 2023-11-23 at 9 43 50 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/fbe659ea-892c-4cf2-bd3e-e318a31a0049">
<img width="1387" alt="Screenshot 2023-11-23 at 9 44 34 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/59699784-ea59-49d2-ad09-95ac9d103533">
<img width="1391" alt="Screenshot 2023-11-23 at 9 46 58 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/5f7d8d31-0848-4f50-b5ce-5ce60f6664eb">
<img width="1310" alt="Screenshot 2023-11-23 at 9 50 55 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/d2bd8dae-1362-489d-a831-afa4c41d39d0">

_________________

3. Statistics:

The Statistics object serves as a storage for the helper functions used to produce the required statistics about the game using GET requests provided by clients. dijkstraShortestPath() function is used to calculate the shortest path to the node where the valuableData attribute is 'true' by using the standard, but effective, Dijktra's algorithm implementation. Next, the reconstructPath() function allows the program to visualize the path taken by the dijkstraShortestPath() function, store it, and later write to the YAML file for user reference. The following functions are essential since on the large graphs without such a 'cheat code' it becomes incredibly difficult to get to the desired node with the valuableData by helplessly going forward in blind. Finally, the calculateConfidenceScore() is used to calculate the confidence score for the current node which tells the user that the node or the edges that lead to it were not perturbed.

# Sample output:

_________________

<img width="389" alt="Screenshot 2023-11-23 at 10 07 57 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/88ddc984-00d3-4987-ad07-200f747d1088">
<img width="346" alt="Screenshot 2023-11-23 at 10 01 47 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/cbff676b-9358-4934-a06c-fe879e751bad">

_________________

4. Behavior:

The primary purpose of the Behavior object is to serve as a storage for functions responsible for handling ultimate conditions of the game, such as win/lose of both clients, restarting the game, displaying the welcome message to users, and initializing the starting positions for Thief and Policeman nodes. The ultimateConditions(0 function encapsulates the logic for checking the basic game conditions which will trigger the restartGame() function stored within the inner gameOverMessage() function to reinitialize the starting positions for each player, and start the game from the beginning. findStartingPositions() function is used to assign each user a starting position on both the original and perturbed graphs. Finally, the welcome() function is used to display the 'welcome' message to the users, as well as to check at the very beginning of the game the conditions which allow to restart the game right after the launch. Such scenarios include the situations, when the Thief was intially assigned a node which contains the valuableData attribute with the 'true' value. In such case the Thief wins by default. In situations when both the Thief and Policeman were placed at the same node, and its valuableData is false, the Policeman wins automatically. Below you can see that both players have been placed at the same node where valuableData is false. Thus, the Policeman wins by default.

# Sample output:

_________________

<img width="1323" alt="Screenshot 2023-11-23 at 10 17 51 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/d7e6c2f7-a7ff-46d9-b51d-f5f8a8c6a39e">


_________________

5. Yaml:

Yaml object contains functions for handling YAML parsing and conversion. The parseNode() function is used to parse the String from the 'curl' command, and transform it into a NodeObject which can be used to detect the adjacent nodes using the corresponding functions for this purpose. Next, the convertMovesToYaml() initializes the HashMap which will store the obtained statistics in the form of key/value pair, and dump the contents into the Yaml object. The following function uses nodeToMap() method to place the headers and corresponding to it data into the Yaml file. Additionally, the moveToMap() function is used to store the sequence of moves taken by each player inside the newly generated Yaml file. Finally, the writeYamlToFile() method is used to write the full information into the Yaml file and close it after the procedure is done. Additionally, in tandem with the Yaml functions within the route of the GameLogic object, Spark functionality is used to produce the statistics about the confidence, adjacent nodes, and the moves taken by each player so far. 

# Sample output:

_________________

<img width="396" alt="Screenshot 2023-11-23 at 10 26 59 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/ed6df442-1882-4940-baf7-8d9c8f2e5fd6">
<img width="677" alt="Screenshot 2023-11-23 at 10 32 01 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/a33cebb8-272f-41c6-a63c-815f713c8104">

_________________


6. DataConverter:

DataConverter object is responsible for converting the graphs into a human-friendly readable format, and for the subsequent sharding of the generated files for the purpose of parallel processing in Map/Reduce model. This time the following object has been used for testing purposes. After receiving the serialized graphs as arguments, the processFile() function begins processing them simultaneously by calling the processFilesToCsv() and processEdgesToCsv() functions to access nodes and edges of the two graphs via using nodes() and edges() function calls respectively. Subsequently, shuffling of objects occurs with each other in order to create all kinds of combinations that will be used as arguments when calculating similarities via SimRank in the Map/Reduce. After generating combinations, the sharder() function is called, which splits the resulting files into shards, the size of which depends on the argument specified when calling this function. All received shards are saved in two specified folders for user convenience.

# Sample output:

____________________

<img width="1320" alt="Screenshot 2023-11-02 at 11 37 58 PM" src="https://github.com/Wondamonstaa/NetRandomWalker/assets/113752537/e88539db-1024-4f8f-bea1-b1868d892a51">

____________________


7. Mixer:

Located under DataManipulation folder, the following helper object was introduced to ease the load on DataConverter object as well as to simplify the method of extracting the data from the graph nodes and edges for further shuffling and subsequent usage of the obtained information. The following object contains 3 methods: 1) Using the exec() method, the Mixer object loads the graphs, accesses their nodes and edges, and invokes the other two object methods; 2) Next, combineAndWriteToCSV() and writeNodesToCSV() methods shuffle the generated files using Apache Spark functionality allowing to read and store the data in the DataFrame object, access it and, using monadic operations, create thousands of possible combinations of original and perturbed counterparts containing the path walked by the RandomWalk algorithm to detect whether the attack of the Man in the Middle was succesful or not.



____________________

8. RandomWalker: 

The primary purpose of the following object is implementation of the RandomWalk algorithm using Apache Spark, a unified analytics engine for large-scale data processing. Main method surves as the entry point of the RandomWalker object. The object contains the driver method called runRandomWalk(), whose goal is to run the main functionality of the class, enabling it to perform the random walks around the provided perturbed graph using the tuned algorithm. Inside the runRandomWalk() method, I initialize the Spark Session via using in-built builder() method which is the he entry point to programming Spark with the Dataset and DataFrame API. Next, the method checks whether the graps' binaries were loaded successfully, and creates an adjacency matrix of the nodes and edges via invoking adjacencyMatrix() method. Following that, the randomWalk() is being invoked with the specified number of required arguments. Inside the randomWalk() method, via using the helper function getRandomNode(), I access the random connected to the graph node which will serve as the starting point of the path we are going to explore. Using the helper explorePath() and the powerful functionality of the Apache Spark engine, via parallelizing computations I begin randomly exploring the graph starting at the initial node, and then connecting to the closest successor via checking the neighbors using adjacencyMatrix() method. The explored path is stored in the Resilient Distributed Dataset, which will be partitioned and saved as a text file containing the result of the walked path after the algorithm finishes executing. The resulting output .txt file contains the path randomWalk() travelled across the graph. 

Next, within the runRandomWalk() method the execution continues by invoking the exec() method of the Mixer object for generating combinations of shards between the path taken by randomWalk() algorithm and the possible scenarios that the agent could have travelled across the original graph. Finally, the result of the exec() function is accessed via the sparkSession object and stored into the Resilient Distributed Dataset of String type. Next, the data stored in the current RDD is accessed by another RDD of type String via using the map() method: every single line of the stored is being accessed, and then calculateSimRank() and calculateSimRankWithTracebilityLinks() methods are being invoked to produce the statistics about the graphs and to detect whether the attack of the Man in the Middle was successful or unsucessful via comparing the properties of nodes and edges, as well as detecting how many honeypots were mistakenly accessed by the attacker. Based on the results of the comparison, and the number of matches between the two, different score is assigned, which is later compared to the selected threshold. As a result, the 'part-00000' file with the statistics is produced for further analysis. Please, keep in mind that this is the file you will need to check in order to obtain the results. The following file will be located at the specified folder for the output files from the selected Apache Spark job.

___________________

# Sample output:

<img width="323" alt="Screenshot 2023-11-03 at 12 11 00 AM" src="https://github.com/Wondamonstaa/NetRandomWalker/assets/113752537/34fcb568-fe04-4b8a-a6f4-9ae89a269cb0">


___________________


9. SimRank:

The following object, as mentioned earlier, is used to calculate the similarities between two graphs by analyzing the properties nodes and edges from both the original and perturbed graphs. Using the specified threshold and the reward system, the algorithm performs the comparison of the data stored in the nodes and edges of both graphs to determine the current similarity score between them which is used for the final decision based on which the one can tell whether or not are two compared objects are the same. Additionally, the following algorithm has been modified the way to keep track of the number of successful and unsuccessful attacks performed by the Man in the Middle while trying to steal the valuable data from the system. The calculations are done by keeping an eye on how many times the honepots, represented by the added nodes in the perturbed graphs about which the attacker does not know, have been accessed. If the honepot has been accessed in order to obtain the data stored there, the attacker will be immediately flagged by the system, and subsequent instructions will be sent to eliminate the threat. Additionally, the following algorithm computes similarity and F-1 score, as well as the statistics about random walks, such as min/max/median/mean number of nodes in these walks and the ratio of the number of random walks resulting in successful attacks to the total number of random walks. Finally, as mentioned previously, the result of the computations will be stored in the 'part-00000' file with the statistics is produced for further analysis.


___________________

# Sample output:

<img width="448" alt="Screenshot 2023-11-03 at 12 21 14 AM" src="https://github.com/Wondamonstaa/NetRandomWalker/assets/113752537/ef7f9dc4-0b4d-45bb-9c9d-b2c81b692683">


___________________



10. NodeSimilarity:

The following object serves its primary role as a helper and the basis for implementing the methods in other objects, including SimRank. The primary goal is to implement additional functionality and methods that will be useful in computing the similarity ratio between nodes and edges using SimRank algorithm.


## Test Cases

The tests can be run using 'sbt clean test' command or by directly invoking the class RouteTest, located under the following path: src/main/Test/RouteTest.scala

In essence, the battery of tests conducted evaluates the performance and functionality of the project, specifically the functionality of the 'curl' commands, path algorithms and its ability clearly and efficiently travel across the nodes and edges of the provided graph, and SimRank and RandomWalk classes, alongside the sharding function, assessing their proficiency in executing tasks accurately and effectively. These tests serve as a robust validation mechanism, ensuring that both components operate seamlessly and in accordance with their intended objectives.


## Limitations:

1) For local program execution, the user needs to have JDK version 1.8, Scala 2.13.10, sbt 1.9.6, Apache Spark 3.5.0, and Akka HTTP 10.5.0 installed.
2) The program supports processing multiple files within the same input folder if the user chooses to split the files, but it cannot manage input files from different locations. 
3) The user should possess the capability to provide Read/Write permissions to the "Users" group for the LogFileGenerator project folder. This typically requires Administrator-level access.
4) The feature for altering the name and extension of the output file is effective only during local execution. In other words, it does not modify the name and extension in the case of program execution on AWS EC2 IaaS, particularly in S3 Bucket.
5) Due to occasional unstable connection with the server and minor bugs with the load() function, the test cases may fail 1/10 times, as well as the disconnection from the web server can occur.
6) To prevent the internal server errors, please, correctly specify the playerId for each player, such as 'thief' or 'policeman' while testing the functionality using 'curl' commands in the terminal or any other resources.


## Farewell note for Dr. Mark Grechanik and Utsav

___________

In the final phase of this project, I conducted a thorough analysis of the results and reflected on the challenges encountered, aiming to extract valuable lessons for future real-world projects in the rapidly evolving Tech industry. Throughout the development process, one significant challenge was the initial lack of comprehension of Akka HTTP, leading to several mistakes that required subsequent correction. However, the chosen algorithm ultimately enabled the establishment of a robust connection to the server during the server development phase, overcoming technical difficulties. Additionally, basic HTTP commands facilitated effective local and cloud environment testing. While the code logic exhibits minor imperfections impacting the overall user experience in terms of information display, these issues are non-critical to the core gameplay. The algorithm for determining the most optimal path consistently provides the user with the shortest route to the node containing the valuableData attribute set to 'true' in 95-100% of cases.

It's essential to acknowledge that not all aspects of the application demonstrate high accuracy. The function responsible for calculating the confidence score occasionally yields false positive results in 15-20% of cases, representing a notable margin of error. However, the process of searching for adjacent nodes to the current node for each player functions as anticipated, generating a list of potential moves for each player. The choices made by players influence the outcomes of both Dijkstra's algorithm and the confidence score calculation. Furthermore, the logic governing win/lose conditions executes as expected, delivering appropriate messages to the user and seamlessly restarting the game by generating new node positions for the Thief and Policeman. This comprehensive evaluation underscores the importance of continuous learning and adaptation in the dynamic field of technology.

As I approach the culmination of my academic journey at UIC, I want to express my heartfelt gratitude to each of you for the incredible hard work and dedication exhibited throughout this semester. The experience has been both unforgettable and priceless, and I am truly grateful to have been part of this enriching adventure! Admittedly, not every step in this journey went as smoothly as planned. Countless hours were invested in mastering new material, navigating the intricacies of cloud environments, deciphering project descriptions, architecting future program structures, and repeatedly grappling with code that seemed determined to defy all efforts. Through it all, there were moments of nervous outbursts and energetic expressions (not directed at any of you, of course!).

Every project undertaken in this course underscored the importance of meticulous project design at the outset and emphasized the value of seeking clarification at the earliest signs of understanding gaps. Despite the challenges, witnessing the growth in my programming skills and gaining a deeper understanding of the structures underlying the code and systems I work with brings immense joy. It's not just a level up; it's a giant leap in personal development, and that's an achievement we all share!

This course serves as a real-world school of life for any student aspiring to grasp the essence of Computer Science. I consider myself fortunate to have been part of this journey. I extend my sincere thanks for the invaluable experience and the wealth of knowledge imparted in this short yet impactful semester, Dr. Mark. Having taken courses at multiple universities and encountered various instructors, I can without any doubts say that your commitment to fostering student growth and the quality of knowledge you provide are truly exceptional! 

Dr. Mark, Utsav, thank you so much for your hard work, time, patience, and understanding with us throughout this semester! Its been a pleasure to meet you, and I hope to see you again!
___________
# PhantomChase
