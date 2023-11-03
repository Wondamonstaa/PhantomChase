# CS 441 Project 2 Fall 2023
## Kiryl Baravikou
### UIN: 656339218
### NetID: kbara5

Repo for the NetRandomWalker Project 2 for CS 441 Fall 2023

---

AWS EMR Deployment YouTube link: EDIT

---

## How to Run the Project:
1) Start by downloading the project repository from Git. Please, note, that the git repository contains the folder TO_USE. The following folder has all the essential resources I have been using to successfully run the project.
2) Navigate to the NetRandomWalker directory: use your terminal or command prompt to navigate to the "NetRandomWalker" directory within the downloaded project folder.
3) Before your run the project, please, make sure that your current setting satisfy the requirements: a) Scala version: 2.13.10; b) JDK version: 1.8; c) SBT version: 1.9.6; d) Apache Spark version: 3.5.0.
4) After all the prerequisites are met, please, go to File -> Project Structure -> Modules -> Add -> JARs or Directories -> select TO_USE folder -> netmodelsim.jar -> select the uploaded module -> set scope to 'Compile' -> click 'Apply' -> click 'OK'. By doing so I ensure that the essential injected binary files will not be missing. The following .jar file allows me to get access to the methods implemented in the original project created by Dr. Mark. Specified setting in the build.sbt allow to mix multiple Scala versions without any errors.
5) In your terminal, run the following command to clean, compile, and execute the project: `sbt clean compile run`. The following command successfully launched the Main class of the program which contains the essential methods for program execution of the written methods within other objects.
6) To run tests and ensure the code's functionality, use the following command: `sbt clean test`. The tests written are located within src/test/scala/RandomWalkerTest path directory. All the tests on my end ran successfully among with tests written by Dr. Mark.
7) If you need to create a .jar file for your project, execute the following command: `sbt clean assembly`. The resulting jar will be placed in __NetGameSim/target/scala-3.2.2/netrandomwalker.jar__. Alternatively, a user can use the manual approach to create a .jar file of the project: File -> Project Structure -> Artifacts -> Add -> JAR -> From modules with dependencies -> Specify the main class of the project -> click 'OK' -> go to Build -> Build Artifacts -> in the “Build Artifacts” dialog window, click on “Build” to generate your JAR file. The generated .jar file will be located in the specified output directory.
8) If you prefer using IntelliJ for development: a) Import the project into IntelliJ IDE. b) Build the project. c) Create a "configuration" for the Main.scala file. d) Set the configuration's arguments to be the input and output folders, separated by a space. NB: to run the project properly, a user, i.e. you, should specify the input/output paths in the application.conf file. The current configurations are set to default values which you may have to change to run the project successfully.
9) Ensure that your local input/output folder has the necessary permissions to allow the program to read from and write to it. You can check and adjust folder permissions by right-clicking on the project directory, selecting "Properties," and navigating to the "Security" tab. Make sure that the group "Users" has both "Read" and "Write" permissions.
10) Before running the program, ensure that Hadoop is running on your machine. The program may depend on Hadoop, so it's essential to have it set up and running correctly.
---

## Requirements:

The goal of the project is to create a program for parallel distributed processing of large graphs to produce matching statistics about generated graphs and their perturbed counterparts, and develop a program for simulating parallel random walks on the generated graphs, with the aim of modeling and understanding the behavior of Man-in-the-Middle attackers. The following core functionalities must be met:

1. Statistics Generation: compute a YAML or CSV file that displays the number of successful attacks and the failed attacks for a given number of iterations. Other metrics can include the statistics about random walks, e.g., min/max/median/mean number of nodes in these walks and the ratio of the number of random walks resulting in successful attacks to the total number of random walks.
2. Random Walk: implement the standard Random Walk algorithm using Apache Spark which simulates a traversal of the graph in which the traversed relationships are chosen at random.
3. Attack simulation: using the implemented Random Walk algorithm, simulate an attack on the large distributed system. The goal of the each iteration is to compute the statistics about the number of successful/unsuccessful attacks. 
4. Custom Graph Matching Algorithm: develop a uniquely fine-tuned graph matching algorithm to make determinations about modifications.
5. Comparison with Golden Set YAML: compare each detected modification with the golden set of changes generated by NetGraphSim when creating a perturbed graph.
6. Algorithm Evaluation: assess the goodness of your algorithm, similar to precision and recall ratios, based on experimentation and results.

Other Requirements:
1) The output files should be in the format of .csv or any other human-readible format.
2) 5 or more scalatests should be implemented.
3) Logging used for all programs.
4) Configurable input and output paths for the Apache Spark programs.
5) Compileable through 'sbt clean compile run'
7) Deployed on AWS EMR demonstrating all the steps of deployment.
8) Video explanation of the deployment recorded.
9) RandomWalk algorithm must be implemented.
10) Sophisticated documentation with clarifications of the chosen design rationale.

---

## Technical Design

This section will provide a detailed description of each of the classes that I implemented in this project, as well as the rationale for using one or another class when planning and developing the design of an algorithm for calculating similarities in the generated graphs:

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

Located under DataManipulation folder, the following helper object was introduced to ease the load on DataConverter object as well as to simplify the method of extracting the data from the graph nodes and edges for further shuffling and subsequent usage of the obtained information. The following object contains 3 methods: 1) Using the exec() method, the Mixer object loads the graphs, accesses their nodes and edges, and invokes the other two object methods; 2) Next, combineAndWriteToCSV() and writeNodesToCSV() methods shuffle the generated files using monadic operations to create thousands of possible combinations of original and perturbed counterparts containing the path walked by the RandomWalk algorithm to detect whether the attack of the Man in the Middle was succesful or not.



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

1) For local program execution, the user needs to have Java 8 or a higher version, sbt 1.6, and Hadoop 3.3.6 installed.
2) The program supports processing multiple files within the same input folder if the user chooses to split the files, but it cannot manage input files from different locations. 
3) The user should possess the capability to provide Read/Write permissions to the "Users" group for the LogFileGenerator project folder. This typically requires Administrator-level access.
4) The feature for altering the name and extension of the output file is effective only during local execution. In other words, it does not modify the name and extension in the case of program execution on AWS EMR, particularly in S3 Bucket.


## Note for Dr. Mark Grechanik and Utsav

___________

The final part of this assignment requires us to compare the produced results with the results produced by Dr. Mark in the YAML file to estimate the goodness of our algorithm. Carrying out this comparison makes sense only if the algorithm is mostly free of gross errors, which minimizes the chance of obtaining an inaccurate result. In my case, the algorithm is imprecise and can sometimes be confusing when analyzing the generated data. When writing the algorithm, due to the lack of a clear understanding of how exactly to work with this kind of data, and due to lack of experience, I did not take into account a number of important factors that play a key role in determining the percentage of similarity between nodes and edges of both graphs. Taking into account all the above, I conclude that my algorithm is inaccurate and cannot be used at this stage for the purpose of accurately comparing the results obtained with the results of Dr. Mark. The implemented algorithm still needs careful refinement and additional improvements in order to increase the accuracy of calculations.

To draw a conclusion, I would like to thank you for the chance to get a feel for a real example of what real programming is like that we will encounter in a real work environment. Realizing the number of hours that were invested in writing this project, and, frankly speaking, the low level of the result obtained, which cannot but upset, this allows me to rethink the approach I use to writing projects. This project made me realize the importance of planning the design of a future project at its earliest stages, as well as the need to ask questions even if there are even the slightest gaps in understanding what you have to work with. A lot of mistakes made when writing the project in its early stages significantly complicated the work process later, which led to the need to rewrite the code over and over again, which, in turn, gave rise to bugs in other areas of the program. Finally, I would like to note once again that the opportunity to work on tasks of this level is invaluable, and subsequently I will work on mistakes in order to prevent them when writing upcoming projects: CS 441 is notorious among students for its complexity, but at the same time it is one of the best if not the best, course at UIC to gain real world experience that will help anyone who completes it become the best version of themselves.

Thank you for your hard work, time, patience, and understanding!
___________
