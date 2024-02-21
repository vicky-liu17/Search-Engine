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
```

$$Precision = \frac{4}{11} \approx 0.36$$

$$Recall = \frac{4}{100} = 0.04$$

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
```
$$Precision = \frac{4}{11} \approx 0.36$$

$$Recall = \frac{4}{100} = 0.04$$

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

- Query: "mathematics program graduate UC""

$$Precision = \frac{0}{1} \approx 0$$

$$Recall = \frac{0}{100} = 0$$


- **why you think that the final query gave better precision and/or recall than the earlier variants?**
- The final query "graduate program mathematics degree UC Davis" likely yields better precision compared to the earlier variant "graduate program mathematics" due to its increased specificity, reduced ambiguity, targeted nature, improved relevance, and better alignment with the user's likely intent. By including terms such as "degree" and "UC Davis," the final query provides additional context and narrows down the search results to specifically match the user's interest in graduate programs in mathematics at UC Davis, resulting in more relevant and targeted information retrieval.

- **Why can we not simply set the query to be the entire information need description?**
- Setting the query to be the entire information need description, such as "Info about the education in Mathematics on a graduate level at UC Davis," might not be optimal for several reasons. Firstly, the query may contain unnecessary or ambiguous terms that could lead to irrelevant search results. Additionally, users may not always articulate their information needs in a concise and clear manner, leading to overly broad queries. Moreover, search engines typically work more effectively with shorter, focused queries that contain key terms directly related to the user's intent, rather than lengthy and verbose descriptions. Lastly, including the entire information need description as the query may limit the search engine's ability to retrieve relevant results by not accounting for variations in language or terminology. Therefore, employing a more concise and targeted query approach is generally preferred for effective information retrieval.