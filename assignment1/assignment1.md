### Task 1.5

```
1 Elaine_Kasimatis.f 1
1 Events_Calendars.f 0
1 Student_Organizations.f 0
1 Quantitative_Biology_and_Bioinformatics.f 0
1 Private_Tutoring.f 0
1 Economics.f 0
1 Biological_Systems_Engineering.f 0
1 UC_Davis_English_Department.f 0
1 Computer_Science.f 0
1 What_I_Wish_I_Knew...Before_Coming_to_UC_Davis_Entomology.f 1
1 Evelyn_Silvia.f 1
1 Fiber_and_Polymer_Science.f 0
1 UCD_Honors_and_Prizes.f 0
1 document_translated.f 0
1 Statistics.f 2
1 University_Departments.f 1
1 ECE_Course_Reviews.f 0
1 Hydrology.f 0
1 MattHh.f 1
1 Candidate_Statements.f 0
1 Wildlife%2C_Fish%2C_and_Conservation_Biology.f 0
1 Mathematics.f 3
```

##### Difficult cases
- 1 Elaine_Kasimatis.f 1 (This is a graduate of a mathematics program, perhaps somewhat relevant to the query?)
- 1 Statistics.f 2 (The description is not about a mathematics program specifically, but statistics is closely related to mathematics. I'm curious if it is "Fairly relevant" or "Marginally relevant".)
- 1 What_I_Wish_I_Knew...Before_Coming_to_UC_Davis_Entomology.f 1 (This document mentions the application information for the Mathematics department, perhaps there is some relevance?)

##### Calculation

$$Precision = \frac{Relevant—retrieved-instances}{All-retrieved-instances} = \frac{7}{22} \approx 0.31$$

$$Recall = \frac{Relevant-retrieved-instances}{All-relevant-instances} = \frac{7}{100} = 0.07$$


### Task 1.6

- Query: "mathematics graduate education" 

```
1 Elaine_Kasimatis.f 1
1 The_Mary_Jeanne_Gilhooly_Award.f 0
1 Events_Calendars.f 0
1 Student_Organizations.f 0
1 Private_Tutoring.f 0
1 Mechanical_and_Aeronautical_Engineering.f 0
1 Graduate_Groups.f 2
1 Evelyn_Silvia.f 1
1 UCD_Honors_and_Prizes.f 0
1 document_translated.f 0
1 University_Departments.f 1
1 Linda_Katehi.f 0
1 Candidate_Statements.f 0
```

$$Precision = \frac{4}{13} \approx 0.31$$

$$Recall = \frac{4}{100} = 0.04$$

- Query: "mathematics graduate course"

```
1 Quantitative_Biology_and_Bioinformatics.f 0
1 Private_Tutoring.f 0
1 Economics.f 0
1 UC_Davis_English_Department.f 0
1 Computer_Science.f 0
1 What_I_Wish_I_Knew...Before_Coming_to_UC_Davis_Entomology.f 1
1 Trivia_Nights.f 0
1 Fiber_and_Polymer_Science.f 0
1 BrandonBarrette.f 0
1 Teaching_Assistants.f 0
1 UCD_Honors_and_Prizes.f 0
1 document_translated.f 0
1 Statistics.f 2
1 ECE_Course_Reviews.f 0
1 Hydrology.f 0
1 MattHh.f 1
1 Mathematics.f 3
```

$$Precision = \frac{4}{17} \approx 0.24$$

$$Recall = \frac{4}{100} = 0.04$$

- Query: "mathematics graduate faculty"

```
1 UC_Davis_English_Department.f 0
1 Computer_Science.f 0
1 What_I_Wish_I_Knew...Before_Coming_to_UC_Davis_Entomology.f 1
1 Graduate_Groups.f 2
1 Evelyn_Silvia.f 1
1 UCD_Honors_and_Prizes.f 0
1 document_translated.f 0
1 Hydrology.f 0
1 Linda_Katehi.f 0
1 Wildlife%2C_Fish%2C_and_Conservation_Biology.f 0
1 Mathematics.f 3
1 PatrickDragon.f 1
```

$$Precision = \frac{5}{12} \approx 0.41$$

$$Recall = \frac{5}{100} = 0.05$$

- Query: "mathematics graduate degree"

```
1 Private_Tutoring.f 0
1 Mechanical_and_Aeronautical_Engineering.f 0
1 UC_Davis_English_Department.f 0
1 What_I_Wish_I_Knew...Before_Coming_to_UC_Davis_Entomology.f 1
1 Graduate_Groups.f 2
1 Fiber_and_Polymer_Science.f 0
1 UCD_Honors_and_Prizes.f 0
1 document_translated.f 0
1 Statistics.f 2
1 Wildlife%2C_Fish%2C_and_Conservation_Biology.f 0
1 Mathematics.f 3
1 PatrickDragon.f 1
```
$$Precision = \frac{5}{12} \approx 0.41$$

$$Recall = \frac{5}{100} = 0.05$$

- Query: graduate program mathematics degree UC davis

```
1 Private_Tutoring.f 0
1 UC_Davis_English_Department.f 0
1 What_I_Wish_I_Knew...Before_Coming_to_UC_Davis_Entomology.f 1
1 UCD_Honors_and_Prizes.f 0
1 Statistics.f 2
1 Wildlife%2C_Fish%2C_and_Conservation_Biology.f 0
1 Mathematics.f 3
```

$$Precision = \frac{3}{11} \approx 0.43$$

$$Recall = \frac{3}{100} = 0.03$$

- Query: "mathematics program graduate UC"

$$Precision = \frac{Relevant—retrieved-instances}{All-retrieved-instances} = \frac{7}{18} \approx 0.39$$

$$Recall = \frac{Relevant-retrieved-instances}{All-relevant-instances} = \frac{7}{100} = 0.07$$

- Query: "Info about the education in Mathematics on a graduate level at UC Davis""

$$Precision = \frac{0}{1} \approx 0$$

$$Recall = \frac{0}{100} = 0$$


- **why you think that the final query gave better precision and/or recall than the earlier variants?**
- The final query "graduate program mathematics degree UC Davis" likely yields better precision compared to the earlier variant "graduate program mathematics" due to its increased specificity, reduced ambiguity, targeted nature, improved relevance, and better alignment with the user's likely intent. By including terms such as "degree" and "UC Davis," the final query provides additional context and narrows down the search results to specifically match the user's interest in graduate programs in mathematics at UC Davis, resulting in more relevant and targeted information retrieval.

- **Why can we not simply set the query to be the entire information need description?**
- Setting the query to be the entire information need description, such as "Info about the education in Mathematics on a graduate level at UC Davis," might not be optimal for several reasons. Firstly, the query may contain unnecessary or ambiguous terms that could lead to irrelevant search results. Additionally, users may not always articulate their information needs in a concise and clear manner, leading to overly broad queries. Moreover, search engines typically work more effectively with shorter, focused queries that contain key terms directly related to the user's intent, rather than lengthy and verbose descriptions. Lastly, including the entire information need description as the query may limit the search engine's ability to retrieve relevant results by not accounting for variations in language or terminology. Therefore, employing a more concise and targeted query approach is generally preferred for effective information retrieval.


### Task 1.7

- When storing the hashmap to disk, we iterate through its keys and save each PostingsList in a data file. Each new PostingsList is placed after all previously stored lists. To map a key to its corresponding PostingsList, we hash the key (token) and store the pointer to the PostingsList in a dictionary file at the hashed position.

- During retrieval, we hash the token, navigate to the hashed position in the dictionary, and inspect the position in the data file to check if it holds the correct PostingsList. If it does, we parse and return it. If not, we move to the next hash slot until we find the correct one (to handle collisions) or until an empty slot is encountered (indicating that no such token exists).

The provided code implements a hash function using the multiplicative hashing method. Here's a brief explanation:

```c
// Define a hash function named HashMultiplicative that takes a key (a character array) and its length as input
UINT HashMultiplicative(const CHAR *key, SIZE_T len) {
   // Initialize the hash value to some initial value (INITIAL_VALUE)
   UINT hash = INITIAL_VALUE;
   
   // Iterate through each character in the key
   for(UINT i = 0; i < len; ++i)
      // Update the hash value using the multiplicative hashing algorithm:
      // 1. Multiply the current hash value by a constant multiplier (M)
      // 2. Add the ASCII value of the current character to the hash value
      hash = M * hash + key[i];
   
   // Take the modulo of the hash value with TABLE_SIZE to ensure it falls within the desired range
   return hash % TABLE_SIZE;
}
```

Explanation:
- The function takes a key (represented as a character array) and its length as input.
- It initializes the hash value to some initial value (INITIAL_VALUE).
- It iterates through each character in the key.
- For each character, it updates the hash value using the multiplicative hashing algorithm, which involves multiplying the current hash value by a constant multiplier (M) and adding the ASCII value of the character.
- Finally, it takes the modulo of the resulting hash value with TABLE_SIZE to ensure that the hash value falls within the desired range.


The constants 7 and 103 are chosen for their effectiveness in generating unique hash values and reducing collisions. 
- The prime number 7 is used as the initial value to distribute hash values evenly across the table. Using a prime number as the initial value helps reduce patterns in multiplication, which can lead to fewer collisions and a more uniform distribution of hash values.
- The prime number 103 is used as the multiplier to amplify differences between characters in the input, aiding in uniform distribution. Prime numbers are preferred as multipliers because they help in spreading out the hash values more uniformly. Additionally, 103 is relatively large enough to amplify differences between the characters in the input string, but not too large to cause overflow issues or excessive computational complexity.