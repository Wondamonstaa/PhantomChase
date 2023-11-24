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
e) the first player with no available moves left loses the game.
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

GraphHolder, located under src/main/scala/Game, this time serves as the entry point of the program. The following object obtains the configurations, specified in the application.conf file, using the ConfigFactory class, and loads the essential resources for the game to play. The entry point of the object is the 'main' method, which was left empty to simplify the program flow and make the objects accessible by the GameLogic and other objects easily throughout the project. After getting the configurations using the globalConfig value of Config type, using the load() method I load the original and perturbed graphs located under the specified paths, which later will be accessed by the GameLogic object to successfully run the server.

# Sample output:

_________________

![WhatsApp Image 2023-09-11 at 10 02 36 PM](https://github.com/Wondamonstaa/NetGameSim_Project1/assets/113752537/208bc260-e8e5-4099-a62d-7e72a49561a1)


_________________

2. GameLogic:

Located under the Game folder, the following object serves its primary role as the core of the game by implementing the logic for the two actors using akka HTTP library: Thief and Policeman. The entry point of the program is the main() method. Inside the following method I initialize the SparkSession using builder() method to access the wide range of tools offered by the Spark library for further usage. Next, I initialized the ActorSystem, which is the home to a hierarchy of Actors. It is created using apply() method from a Behavior object that describes the root Actor of this hierarchy and which will create all other Actors beneath it. Following the ActorSystem, the essential ExecutionContext was created which allows to execute program logic asynchronously. The route of the game with the specified path contains the basic rules and conditions that must be met for normal program flow. The two basic methods used to effectively play the game are defined and implemented within the route: PUT and GET. PUT allows each client to send a request to the server using the 'curl' command, which results in a player making a move to the selected node from the list of adjacent nodes to the current node, while the GET method allows to query the perturbed graph in order to obtain the essential statistics about our own and opponent's adjacent nodes, the shortest path to the node with the valuableData attribute, the confidence score for the current node, and the current list of moves that players have taken so far. Using the GET method, users can obtain the information in the form of a Yaml file, and, additionally, using the Apache Spark library by representing the data in the form of Datasets of String type for further analysis. Besides that, the GameLogic object contains a variety of case classes used to store and access the data collected by the server, and obtained by users using GET command. The responsible case class for providing the data to users during the query is the GetMoves(playerId: String, replyTo: ActorRef[Moves]) case class, which sends the reply to the specified actor under user request. Additioanlly, the RootJsonFormat was used to simplify the game flow and improve the data readibility for users. The GameLogic object contains a nested Puppets object, that stores the current location of both players as well as initializes the starting position of each player using the findStartingPositions() method. Puppets object also specifies the behavior of each actor by implementing the apply() and applyBehavior() methods which define the initial behavior for the actors, and the methods to invoke when reacting to the user 'curl' commands. Finally, two objects representing actors of the game, ThiefActor and PolicemanActor, contain the basic behavior of each actor.

# Sample output:

_________________

<img width="1320" alt="Screenshot 2023-11-23 at 9 43 50 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/fbe659ea-892c-4cf2-bd3e-e318a31a0049">
<img width="1391" alt="Screenshot 2023-11-23 at 9 46 58 PM" src="https://github.com/Wondamonstaa/PhantomChase/assets/113752537/5f7d8d31-0848-4f50-b5ce-5ce60f6664eb">

_________________


1. Main: 

Located under src/main/scala, the Main object serves as a starting point of the program. This object includes all the necessary imported libraries and modules that allow the code to work safely without any issues. The entry point of the object is the 'main' function, within which via using the ConfigFactory class, I access the specified configration for the program located under application.conf file. After the configurations loading has successfully completed, the provided algorithm locates the provided input files, accesses them and sends the data to the ArgumentParser object, which is responsible for collecting the necessary arguments for subsequent cascading calls to the functions of this project. Inside the ArgumentParser object, using the parse() method, the object accesses the provided input files, and, finally, passes them to the RandomWalker object via accessing runRandomWalk() method of that object that further loads the generated graphs by deserializing the provided .ngs files. Below you can see an example of a generated graph containing 8000 nodes.

# Sample output:

_________________

![WhatsApp Image 2023-09-11 at 10 02 36 PM](https://github.com/Wondamonstaa/NetGameSim_Project1/assets/113752537/208bc260-e8e5-4099-a62d-7e72a49561a1)


_________________


2. DataConverter:

DataConverter object is responsible for converting the graphs into a human-friendly readable format, and for the subsequent sharding of the generated files for the purpose of parallel processing in Map/Reduce model. After receiving the serialized graphs as arguments, the processFile() function begins processing them simultaneously by calling the processFilesToCsv() and processEdgesToCsv() functions to access nodes and edges of the two graphs via using nodes() and edges() function calls respectively. Subsequently, shuffling of objects occurs with each other in order to create all kinds of combinations that will be used as arguments when calculating similarities via SimRank in the Map/Reduce. After generating combinations, the sharder() function is called, which splits the resulting files into shards, the size of which depends on the argument specified when calling this function. All received shards are saved in two specified folders for user convenience.

# Sample output:

____________________

<img width="1320" alt="Screenshot 2023-11-02 at 11 37 58 PM" src="https://github.com/Wondamonstaa/NetRandomWalker/assets/113752537/e88539db-1024-4f8f-bea1-b1868d892a51">



____________________


3. Mixer:

Located under DataManipulation folder, the following helper object was introduced to ease the load on DataConverter object as well as to simplify the method of extracting the data from the graph nodes and edges for further shuffling and subsequent usage of the obtained information. The following object contains 3 methods: 1) Using the exec() method, the Mixer object loads the graphs, accesses their nodes and edges, and invokes the other two object methods; 2) Next, combineAndWriteToCSV() and writeNodesToCSV() methods shuffle the generated files using Apache Spark functionality allowing to read and store the data in the DataFrame object, access it and, using monadic operations, create thousands of possible combinations of original and perturbed counterparts containing the path walked by the RandomWalk algorithm to detect whether the attack of the Man in the Middle was succesful or not.



____________________

4. RandomWalker: 

The primary purpose of the following object is implementation of the RandomWalk algorithm using Apache Spark, a unified analytics engine for large-scale data processing. Main method surves as the entry point of the RandomWalker object. The object contains the driver method called runRandomWalk(), whose goal is to run the main functionality of the class, enabling it to perform the random walks around the provided perturbed graph using the tuned algorithm. Inside the runRandomWalk() method, I initialize the Spark Session via using in-built builder() method which is the he entry point to programming Spark with the Dataset and DataFrame API. Next, the method checks whether the graps' binaries were loaded successfully, and creates an adjacency matrix of the nodes and edges via invoking adjacencyMatrix() method. Following that, the randomWalk() is being invoked with the specified number of required arguments. Inside the randomWalk() method, via using the helper function getRandomNode(), I access the random connected to the graph node which will serve as the starting point of the path we are going to explore. Using the helper explorePath() and the powerful functionality of the Apache Spark engine, via parallelizing computations I begin randomly exploring the graph starting at the initial node, and then connecting to the closest successor via checking the neighbors using adjacencyMatrix() method. The explored path is stored in the Resilient Distributed Dataset, which will be partitioned and saved as a text file containing the result of the walked path after the algorithm finishes executing. The resulting output .txt file contains the path randomWalk() travelled across the graph. 

Next, within the runRandomWalk() method the execution continues by invoking the exec() method of the Mixer object for generating combinations of shards between the path taken by randomWalk() algorithm and the possible scenarios that the agent could have travelled across the original graph. Finally, the result of the exec() function is accessed via the sparkSession object and stored into the Resilient Distributed Dataset of String type. Next, the data stored in the current RDD is accessed by another RDD of type String via using the map() method: every single line of the stored is being accessed, and then calculateSimRank() and calculateSimRankWithTracebilityLinks() methods are being invoked to produce the statistics about the graphs and to detect whether the attack of the Man in the Middle was successful or unsucessful via comparing the properties of nodes and edges, as well as detecting how many honeypots were mistakenly accessed by the attacker. Based on the results of the comparison, and the number of matches between the two, different score is assigned, which is later compared to the selected threshold. As a result, the 'part-00000' file with the statistics is produced for further analysis. Please, keep in mind that this is the file you will need to check in order to obtain the results. The following file will be located at the specified folder for the output files from the selected Apache Spark job. For further explanation of the obtained results, please, refer to the last part of the report called "Note for Dr. Mark Grechanik and Utsav". 

___________________

# Sample output:

<img width="323" alt="Screenshot 2023-11-03 at 12 11 00 AM" src="https://github.com/Wondamonstaa/NetRandomWalker/assets/113752537/34fcb568-fe04-4b8a-a6f4-9ae89a269cb0">





___________________


5. SimRank:

The following object, as mentioned earlier, is used to calculate the similarities between two graphs by analyzing the properties nodes and edges from both the original and perturbed graphs. Using the specified threshold and the reward system, the algorithm performs the comparison of the data stored in the nodes and edges of both graphs to determine the current similarity score between them which is used for the final decision based on which the one can tell whether or not are two compared objects are the same. Additionally, the following algorithm has been modified the way to keep track of the number of successful and unsuccessful attacks performed by the Man in the Middle while trying to steal the valuable data from the system. The calculations are done by keeping an eye on how many times the honepots, represented by the added nodes in the perturbed graphs about which the attacker does not know, have been accessed. If the honepot has been accessed in order to obtain the data stored there, the attacker will be immediately flagged by the system, and subsequent instructions will be sent to eliminate the threat. Additionally, the following algorithm computes similarity and F-1 score, as well as the statistics about random walks, such as min/max/median/mean number of nodes in these walks and the ratio of the number of random walks resulting in successful attacks to the total number of random walks. Finally, as mentioned previously, the result of the computations will be stored in the 'part-00000' file with the statistics is produced for further analysis.


___________________

# Sample output:

<img width="448" alt="Screenshot 2023-11-03 at 12 21 14 AM" src="https://github.com/Wondamonstaa/NetRandomWalker/assets/113752537/ef7f9dc4-0b4d-45bb-9c9d-b2c81b692683">






___________________



6. EdgeRDDSuite:

Inside the EdgeRDDSuite object, the LocalSparkContext trait is used to manage a local SparkContext variable, primarily in the context of testing. It ensures that a new SparkContext is created, used within a specified function, and then correctly stopped after the function's execution. The following object is useful for running Spark-related tests and ensuring that the SparkContext doesn't leak or interfere with other tests. Besides that, the EdgeRDDSuite object contains the helper tripleWalk() method which helps to perform the RandomWalk algorithm on the input graph using parallelization and triplets in combination with monadic operations to maximize the efficiency and resource usage of the machine. The results are stored in the Resilient Distributes Datasets, collected, and, finally, stored in the specified output directory for further analysis.


_______________

# Sample output:

<img width="348" alt="Screenshot 2023-11-03 at 12 50 55 AM" src="https://github.com/Wondamonstaa/NetRandomWalker/assets/113752537/312eaadd-fb9b-42ee-8b66-908f89f772e7">


________________


7. NodeSimilarity:

The following object serves its primary role as a helper and the basis for implementing the methods in other objects, including SimRank. The primary goal is to implement additional functionality and methods that will be useful in computing the similarity ratio between nodes and edges using SimRank algorithm.


## Test Cases

The tests can be run using 'sbt clean test' command or by directly invoking the class SimRankTest, located under the following path: src/main/Test/RandomWalkerTest.scala

In essence, the battery of tests conducted evaluates the performance and functionality of the RandomWalker, specifically the functionality of the RandomWalk algorithm and its ability clearly and efficiently travel across the nodes and edges of the provided graph, and SimRank classes, alongside the sharding function, assessing their proficiency in executing tasks accurately and effectively. These tests serve as a robust validation mechanism, ensuring that both components operate seamlessly and in accordance with their intended objectives.


## Limitations:

1) For local program execution, the user needs to have JDK version 1.8, Scala 2.13.10, sbt 1.9.6, and Apache Spark 3.5.0 installed.
2) The program supports processing multiple files within the same input folder if the user chooses to split the files, but it cannot manage input files from different locations. 
3) The user should possess the capability to provide Read/Write permissions to the "Users" group for the LogFileGenerator project folder. This typically requires Administrator-level access.
4) The feature for altering the name and extension of the output file is effective only during local execution. In other words, it does not modify the name and extension in the case of program execution on AWS EC2 IaaS, particularly in S3 Bucket.


## Note for Dr. Mark Grechanik and Utsav

___________

As before, the final phase of this project involves summarizing the results and reflecting on the mistakes made, aiming to avoid them in future projects. Due to the less precise implementation of the random walk algorithm and errors in the SimRank algorithm calculations, the obtained results may not accurately depict the real attack patterns within the system. The algorithm's imprecision can occasionally make data analysis confusing. Furthermore, the presence of false negatives and false positives significantly impacts the algorithm's accuracy, highlighting the need for substantial improvements in its core components, including logic and overall structure.

Based on the current results, it's evident that only around 22-25% of attacks on the system are unsuccessful, indicating errors in the current algorithm. During the algorithm's development, the lack of clear comprehension of working with this data type, combined with limited experience, led to the oversight of critical factors influencing the similarity between nodes and edges in both graphs. Therefore, considering the errors and inaccuracies in the project's implementation, it's clear that the use of this algorithm is inadequate for assessing the actual threat of attacks on company systems. The algorithm requires thorough refinement and additional enhancements to enhance calculation accuracy.

In conclusion, I want to express my gratitude to Dr. Mark and Utsav for providing the opportunity to experience real-world programming scenarios that we'll encounter in professional work environments. Sometimes, investing numerous hours in a project that ultimately fails to meet its required functionality can be likened to a character in a suspenseful movie, such as "The Silence of the Lambs," where I take on the role of a "lamb" led to the slaughter. However, despite the challenges, I can clearly see the substantial growth in my skills since the beginning of this course. The experience gained is undeniably the most valuable reward.

Every project undertaken in this course reinforces the importance of early-stage project design and the necessity of seeking clarification when even the slightest gaps in understanding exist. Many errors made during the project's early stages complicated the development process and necessitated code rewriting, which, in turn, led to bugs in other parts of the program. In conclusion, the opportunity to work on tasks of this caliber is invaluable, and I am committed to learning from these mistakes to prevent their recurrence in future projects.

Thank you for your hard work, time, patience, and understanding!
___________
# PhantomChase
