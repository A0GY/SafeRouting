University of Reading

Department of Computer Science

Computer Science Undergraduate Report Template

and Report Writing Guide

FirstName(s) LastName
Supervisor: Supervisor’s Name
A report submitted in partial fulfilment of the requirements of
the University of Reading for the degree of
Bachelor of Science in Computer Science
March 28, 2025
Declaration
I, Firstname(s) Lastname, of the Department of Computer Science, University of Reading,
confirm that this is my own work and figures, tables, equations, code snippets, artworks, and
illustrations in this report are original and have not been taken from any other person’s work,
except where the works of others have been explicitly acknowledged, quoted, and referenced.
I understand that if failing to do so will be considered a case of plagiarism. Plagiarism is a
form of academic misconduct and will be penalised accordingly.

I give consent to a copy of my report being shared with future students as an exemplar.

I give consent for my work to be made available more widely to members of UoR and public
with interest in teaching, learning and research.

Firstname(s) Lastname
March 28, 2025
i
Abstract
This is a project report template, including instructions on how to write a report. It also has
some useful examples to use L A TEX. Do read this template carefully. The number of chapters
and their titles may vary depending on the type of project and personal preference. Section
titles in this template are illustrative should be updated accordingly. For example, sections
named “A section...” and “Example of ...” should be updated. The number of sections in
each chapter may also vary. This template may or may not suit your project. Discuss the
structure of your report with your supervisor.

Guidance on abstract writing: An abstract is a summary of a report in a single paragraph up
to a maximum of 250 words. An abstract should be self-contained, and it should not refer to
sections, figures, tables, equations, or references. An abstract typically consists of sentences
describing the following four parts: (1) introduction (background and purpose of the project),
(2) methods, (3) results and analysis, and (4) conclusions. The distribution of these four
parts of the abstract should reflect the relative proportion of these parts in the report itself.
An abstract starts with a few sentences describing the project’s general field, comprehensive
background and context, the main purpose of the project; and the problem statement. A
few sentences describe the methods, experiments, and implementation of the project. A few
sentences describe the main results achieved and their significance. The final part of the
abstract describes the conclusions and the implications of the results to the relevant field.

Keywords: a maximum of five keywords/keyphrase separated by commas

Report’s total word count: we expect a maximum of 20,000 words (excluding reference
and appendices) and about 50 - 60 pages. [A good project report can also be written in
approximately 10,000 words.]

ii
Acknowledgements
An acknowledgements section is optional. You may like to acknowledge the support and help
of your supervisor(s), friends, or any other person(s), department(s), institute(s), etc. If you
have been provided specific facility from department/school acknowledged so.

iii
Contents
List of Figures vi

1 Introduction List of Tables vii
1.1 Background
1.2 Problem statement
1.3 Aims and objectives
1.4 Solution approach
1.4.1 A subsection
1.4.2 A subsection
1.5 Summary of contributions and achievements
1.6 Organization of the report
2 Literature Review
2.1 Example of in-text citation of references in L A TEX
2.1.1 Reference Resources
2.1.2 Changing Bibliography Styles
2.2 Avoiding unintentional plagiarism
2.3 Critique of the review
2.4 Summary
3 Methodology
3.1 Examples of the sections of a methodology chapter
3.1.1 Example of a software/Web development main text structure
3.1.2 Example of an algorithm analysis main text structure
3.1.3 Example of an application type main text structure
3.1.4 Example of a science lab-type main text structure
3.1.5 Legal Social and Ethical considerations
3.2 Example of an Equation in L A TEX
3.3 Example of a Figure in L A TEX
3.4 Example of an algorithm in L A TEX
3.5 Example of code snippet in L A TEX
3.6 Example of in-text citation style
- the text 3.6.1 Example of the equations and illustrations placement and reference in
3.6.2 Example of the equations and illustrations style
3.6.3 Tools for In-text Referencing
3.7 Summary
4 Results CONTENTS v
4.1 A section
4.2 Example of a Table in L A TEX
4.3 Example of captions style
4.4 Summary
5 Discussion and Analysis
5.1 A section
5.2 Significance of the findings
5.3 Limitations
5.4 Summary
6 Conclusions and Future Work
6.1 Conclusions
6.2 Future work
7 Reflection
References
Appendices
A An Appendix Chapter (Optional)
B An Appendix Chapter (Optional)
List of Figures
3.1 Example figure in L A TEX.............................. 10
vi
List of Tables
3.1 Undergraduate report template structure.................... 6
3.2 Example of a software engineering-type report structure............ 7
3.3 Example of an algorithm analysis type report structure............ 7
3.4 Example of an application type report structure................ 8
3.5 Example of a science lab experiment-type report structure.......... 8
4.1 Example of a table in L A TEX........................... 15
vii
List of Abbreviations
SMPCS School of Mathematical, Physical and Computational Sciences

viii
Chapter 1
Introduction
Guidance on introduction chapter writing: Introductions are written in the following parts:

A brief description of the investigated problem.
A summary of the scope and context of the project, i.e., what is the background
of the topic/problem/application/system/algorithm/experiment/research question/hy-
pothesis/etc. under investigation/implementation/development [whichever is applicable
to your project].
The aims and objectives of the project.
A description of the problem and the methodological approach adopted to solve the
problem.
A summary of the most significant outcomes and their interpretations.
Organization of the report.
Consult your supervisor to check the content of the introduction chapter. In this tem-
plate, we only offer basic sections of an introduction chapter. It may not be complete and
comprehensive. Writing a report is a subjective matter, and a report’s style and structure
depend on the “type of project” as well as an individual’s preference. This template suits the
following project paradigms:
software engineering and software/web application development;
algorithm implementation, analysis and/or application;
science lab (experiment); and
pure theoretical development (not mention extensively).
Use only a single font for the body text. We recommend using a clean and electronic
document friendly font like Arial or Calibri for MS-word (If you create a report in MS word). If
you use this template, DO NOT ALTER the template’s default font “amsfont default computer
modern”. The default L A TEX font “computer modern” is also acceptable.
The recommended body text font size is minimum 11pt and minimum one-half line
spacing. The recommended figure/table caption font size is minimum 10pt. The footnote^1
font size is minimum 8pt. DO NOT ALTER the font setting of this template.
(^1) Example footnote: footnotes are useful for adding external sources such as links as well as extra information
on a topic or word or sentence. Use command \footnote{...} next to a word to generate a footnote in L A TEX.
1

CHAPTER 1. INTRODUCTION 2
1.1 Background
Describe to a reader the context of your project. That is, what is your project and what its mo-
tivation. Briefly explain the major theories, applications, and/or products/systems/algorithms
whichever is relevant to your project.
Cautions: Do not say you choose this project because of your interest, or your supervisor
proposed/suggested this project, or you were assigned this project as your final year project.
This all may be true, but it is not meant to be written here.

1.2 Problem statement
This section describes the investigated problem in detail. You can also have a separate
chapter on “Problem articulation.” For some projects, you may have a section like “Research
question(s)” or “Research Hypothesis” instead of a section on “Problem statement.’

1.3 Aims and objectives
Describe the “aims and objectives” of your project.
Aims: The aims tell a reader what you want/hope to achieve at the end of the project.
The aims define your intent/purpose in general terms.
Objectives: The objectives are a set of tasks you would perform in order to achieve the
defined aims. The objective statements have to be specific and measurable through the results
and outcome of the project.

1.4 Solution approach
Briefly describe the solution approach and the methodology applied in solving the set aims
and objectives.
Depending on the project, you may like to alter the “heading” of this section. Check with
you supervisor. Also, check what subsection or any other section that can be added in or
removed from this template.

1.4.1 A subsection 1
You may or may not need subsections here. Depending on your project’s needs, add two or
more subsection(s). A section takes at least two subsections.

1.4.2 A subsection 2
Depending on your project’s needs, add more section(s) and subsection(s).

A subsection 1 of a subsection

The command \subsubsection{} creates a paragraph heading in L A TEX.

A subsection 2 of a subsection

Write your text here...

CHAPTER 1. INTRODUCTION 3
1.5 Summary of contributions and achievements
Describe clearly what you have done/created/achieved and what the major results and their
implications are.

1.6 Organization of the report
Describe the outline of the rest of the report here. Let the reader know what to expect ahead
in the report. Describe how you have organized your report.
Example: how to refer a chapter, section, subsection. This report is organised into
seven chapters. Chapter 2 details the literature review of this project. In Section 3...
Note: Take care of the word like “Chapter,” “Section,” “Figure” etc. before the L A TEX com-
mandref{}. Otherwise, a sentence will be confusing. For example, In 2 literature review
is described. In this sentence, the word “Chapter” is missing. Therefore, a reader would not
know whether 2 is for a Chapter or a Section or a Figure. For more information on automated
tools to assist in this work, see Section 3.6.3.

Chapter 2
Literature Review
A literature review chapter can be organized in a few sections with appropriate titles. A
literature review chapter will contain the following:

A review of the state-of-the-art (include theories and solutions) of the field of research.
A description of the project in the context of existing literature and products/systems.
An analysis of how the review is relevant to the intended application/system/problem.
A demonstration of awareness of social, ethical and legal aspects connected to the
field of the project (for instance, machine learning, artificial intelligence, inference and
prediction, ...).
A critique of existing work compared with the intended work.
Note that your literature review should demonstrate the significance of the project.

2.1 Example of in-text citation of references in L A TEX
The references in a report relate your content with the relevant sources, papers, and the works
of others. To include references in a report, we cite them in the texts. In MS-Word, EndNote,
or MS-Word references, or plain text as a list can be used. Similarly, in L A TEX, you can use
the “thebibliography” environment, which is similar to the plain text as a list arrangement like
the MS word. However, In L A TEX, the most convenient way is to use the BibTex, which takes
the references in a particular format [see references.bib file of this template] and lists them in
style [APA, Harvard, etc.] as we want with the help of proper packages.
These are the examples of how to cite external sources, seminal works, and research
papers. In L A TEX, if you use “ BibTex ” you do not have to worry much since the proper use
of a bibliographystyle package like “agsm for the Harvard style” and little rectification of the
content in a BiBText source file [In this template, BibTex are stored in the “references.bib”
file], we can conveniently generate a reference style.
Take a note of the commands \cite{} and \citep{}. The command \cite{} will write like
“Author et al. (2019)” style for Harvard, APA and Chicago style. The command \citep{}
will write like “(Author et al., 2019).” Depending on how you construct a sentence, you need
to use them smartly. Check the examples of in-text citation of sources listed here [This
Department recommends the Harvard style of referencing.]:

Kottwitz (2021) has written a comprehensive guide on writing in L A TEX [Example of
\cite{} ].
4
CHAPTER 2. LITERATURE REVIEW 5
If L A TEX is used efficiently and effectively, it helps in writing a very high-quality project
report (Lamport, 1994) [Example of \citep{} ].
A detailed APA, Harvard, and Chicago referencing style guide are available in (University
of Reading, 2023 b ).
This is an example of how to construct a numbered list in L A TEX, and it includes in-text, named
and parenthetical citations:

Kottwitz (2021) has written a comprehensive guide on writing in L A TEX.
If L A TEXis used efficiently and effectively, it helps in writing a very high-quality project
report (Lamport, 1994).
2.1.1 Reference Resources
You can find additional referencing resources from the University Library:

https://libguides.reading.ac.uk/computer-science
https://libguides.reading.ac.uk/citing-references/citationexamples
2.1.2 Changing Bibliography Styles
While this report used name and date formatting in the Harvard style, you might also wish to
use a numbered style like that from IEEE. To enable this change, you will need to edit theCS_
report.styfile. Uncomment the 2 lines of Harvard settings underBibliography/References
settings, and enable the IEEE style:

% IEEE, Numbered Style
\usepackage[numbers]{natbib}
\bibliographystyle{IEEEtran}

Note that when making this change, you will need to modify the way in which you refer
to authors by name, as this is no longer immediately automatic. Instead, you will need to
additionally rely on\citeauthor{}.

2.2 Avoiding unintentional plagiarism
Using other sources, ideas, and material always bring with it a risk of unintentional plagiarism.
MUST : do read the university guidelines on the definition of plagiarism as well as the guidelines
on how to avoid plagiarism (University of Reading, 2023 a ; Lamport, 1994).

2.3 Critique of the review
Describe your main findings and evaluation of the literature.

2.4 Summary
Write a summary of this chapter

Chapter 3
Methodology
We mentioned in Chapter 1 that a project report’s structure could follow a particular paradigm.
Hence, the organization of a report (effectively the Table of Content of a report) can vary
depending on the type of project you are doing. Check which of the given examples suit your
project. Alternatively, follow your supervisor’s advice.

3.1 Examples of the sections of a methodology chapter
A general report structure is summarised (suggested) in Table 3.1. Table 3.1 describes that,
in general, a typical report structure has three main parts: (1) front matter, (2) main text,
and (3) end matter. The structure of the front matter and end matter will remain the same
for all the undergraduate final year project report. However, the main text varies as per the
project’s needs.

Table 3.1: Undergraduate report template structure
Frontmatter
Title Page
Abstract
Acknowledgements
Table of Contents
List of Figures
List of Tables
List of Abbreviations
Main text
Chapter 1 Introduction
Chapter 2 Literature Review
Chapter 3 Methodology
Chapter 4 Results
Chapter 5 Discussion and Analysis
Chapter 6 Conclusions and Future Work
Chapter 7 Refection
End matter
References
Appendices (Optional)
Index (Optional)
6
CHAPTER 3. METHODOLOGY 7
3.1.1 Example of a software/Web development main text structure
Notice that the “methodology” Chapter of Software/Web development in Table 3.2 takes a
standard software engineering paradigm (approach). Alternatively, these suggested sections
can be the chapters of their own. Also, notice that “Chapter 5” in Table 3.2 is “Testing and
Validation” which is different from the general report template mentioned in Table 3.1. Check
with your supervisor if in doubt.

Table 3.2: Example of a software engineering-type report structure
Chapter 1 Introduction
Chapter 2 Literature Review
Chapter 3 Methodology
Requirements specifications
Analysis
Design
Implementations
Chapter 4 Testing and Validation
Chapter 5 Results and Discussion
Chapter 6 Conclusions and Future Work
Chapter 7 Reflection
3.1.2 Example of an algorithm analysis main text structure
Some project might involve the implementation of a state-of-the-art algorithm and its per-
formance analysis and comparison with other algorithms. In that case, the suggestion in
Table 3.3 may suit you the best.

Table 3.3: Example of an algorithm analysis type report structure
Chapter 1 Introduction
Chapter 2 Literature Review
Chapter 3 Methodology
Algorithms descriptions
Implementations
Experiments design
Chapter 4 Results
Chapter 5 Discussion and Analysis
Chapter 6 Conclusion and Future Work
Chapter 7 Reflection
3.1.3 Example of an application type main text structure
If you are applying some algorithms/tools/technologies on some problems/datasets/etc., you
may use the methodology section prescribed in Table 3.4.

CHAPTER 3. METHODOLOGY 8
Table 3.4: Example of an application type report structure
Chapter 1 Introduction
Chapter 2 Literature Review
Chapter 3 Methodology
Problems (tasks) descriptions
Algorithms/tools/technologies/etc. descriptions
Implementations
Experiments design and setup
Chapter 4 Results
Chapter 5 Discussion and Analysis
Chapter 6 Conclusion and Future Work
Chapter 7 Reflection
3.1.4 Example of a science lab-type main text structure
If you are doing a science lab experiment type of project, you may use the methodology section
suggested in Table 3.5. In this kind of project, you may refer to the “Methodology” section
as “Materials and Methods.”

Table 3.5: Example of a science lab experiment-type report structure
Chapter 1 Introduction
Chapter 2 Literature Review
Chapter 3 Materials and Methods
Problems (tasks) description
Materials
Procedures
Implementations
Experiment set-up
Chapter 4 Results
Chapter 5 Discussion and Analysis
Chapter 6 Conclusion and Future Work
Chapter 7 Reflection
3.1.5 Legal Social and Ethical considerations
This section addresses ethical aspects of your project. This may include: informed consent,
describing how participants will be informed about the study’s purpose, procedures, risks, and
benefits. You should detail the process used for obtaining consent and ensuring participants
understand their rights.

Informed Consent : If data was collected from participant, detail the process for ob-
taining consent and ensuring participants understand their rights.
Confidentiality and Privacy : Explain measures taken to protect participants’ data
and maintain confidentiality. Discuss how data is stored, who will have access, and how
anonymity will be preserved.
CHAPTER 3. METHODOLOGY 9
Risk Assessment : Identify potential risks to participants and outline strategies to
minimize them.
Vulnerable Populations : If applicable, address how you will protect vulnerable groups
(e.g., children, elderly, or marginalized communities) involved in your project.
Research Integrity : Highlight your commitment to honesty and transparency in re-
search. Discuss how you will avoid plagiarism, fabrication, and falsification of data.
Compliance with Regulations : Mention relevant ethical guidelines and regulations
that your project will adhere to.
Impact on Society : Reflect on the broader implications of your project. Discuss how
the outcomes may affect communities, stakeholders, or the environment, and how you
plan to address any potential negative consequences.
Feedback Mechanisms : Describe how you incorporate feedback from participants and
stakeholders to improve the ethical conduct of the project throughout its duration.
3.2 Example of an Equation in L A TEX
Eq. 3.1 [note that this is an example of an equation’s in-text citation] is an example of an
equation in L A TEX. In Eq. (3.1), s is the mean of elements xi ∈ x :

s =
1
N
X N
i =
xi. (3.1)
Have you noticed that all the variables of the equation are defined using the in-text maths
command $.$, and Eq. (3.1) is treated as a part of the sentence with proper punctuation?
Always treat an equation or expression as a part of the sentence.

3.3 Example of a Figure in L A TEX
Figure 3.1 is an example of a figure in L A TEX. For more details, check the link:
wikibooks.org/wiki/LaTeX/Floats,_Figures_and_Captions.
Keep your artwork (graphics, figures, illustrations) clean and readable. At least 300dpi is a
good resolution of a PNG format artwork. However, an SVG format artwork saved as a PDF
will produce the best quality graphics. There are numerous tools out there that can produce
vector graphics and let you save that as an SVG file and/or as a PDF file. One example of
such a tool is the “Flow algorithm software”. Here is the link for that: flowgorithm.org.

CHAPTER 3. METHODOLOGY 10
Main
End
Input
If
Call
False True
Figure 3.1: Example figure in L A TEX.
CHAPTER 3. METHODOLOGY 11
3.4 Example of an algorithm in L A TEX
Algorithm 1 is a good example of an algorithm in L A TEX.
Algorithm 1 Example caption: sum of all even numbers
Input: x = x 1 , x 2 ,.. ., xN
Output: E v enS um (Sum of even numbers in x )
1: function EvenSummation( x )
2: E v enS um ← 0
3: N ← l eng t h ( x )
4: for i ← 1 to N do
5: if xi mod 2 == 0 then ▷Check whether a number is even.
6: E v enS um ← E v enS um + xi
7: end if
8: end for
9: return E v enS um
10: end function
3.5 Example of code snippet in L A TEX
Code Listing 3.1 is a good example of including a code snippet in a report. While using code
snippets, take care of the following:
do not paste your entire code (implementation) or everything you have coded. Add
code snippets only.
The algorithm shown in Algorithm 1 is usually preferred over code snippets in a techni-
cal/scientific report.
Make sure the entire code snippet or algorithm stays on a single page and does not
overflow to another page(s).
Here are three examples of code snippets for three different languages (Python, Java, and
CPP) illustrated in Listings 3.1, 3.2, and 3.3 respectively.
1 import numpy as np
2
3 x = [0, 1, 2, 3, 4, 5] # assign values to an array
4 evenSum = evenSummation(x) # call a function
5
6 def evenSummation(x):
7 evenSum = 0
8 n = len(x)
9 for i in range(n):
10 if np.mod(x[i],2) == 0: # check if a number is even?
11 evenSum = evenSum + x[i]
12 return evenSum

Listing 3.1: Code snippet in L A TEX and this is a Python code example
Here we used the “\clearpage” command and forced-out the second listing example onto
the next page.
CHAPTER 3. METHODOLOGY 12
1 public class EvenSum{
2 public static int evenSummation(int[] x){
3 int evenSum = 0;
4 int n = x.length;
5 for(int i = 0; i < n; i++){
6 if(x[i]%2 == 0){ // check if a number is even?
7 evenSum = evenSum + x[i];
8 }
9 }
10 return evenSum;
11 }
12 public static void main(String [] args){
13 int[] x = {0, 1, 2, 3, 4, 5}; // assign values to an array
14 int evenSum = evenSummation(x);
15 System.out.println(evenSum);
16 }
17 }

Listing 3.2: Code snippet in L A TEX and this is a Java code example
1 int evenSummation(int x[]){
2 int evenSum = 0;
3 int n = sizeof(x);
4 for(int i = 0; i < n; i++){
5 if(x[i]%2 == 0){ // check if a number is even?
6 evenSum = evenSum + x[i];
7 }
8 }
9 return evenSum;
10 }
11
12 int main(){
13 int x[] = {0, 1, 2, 3, 4, 5}; // assign values to an array
14 int evenSum = evenSummation(x);
15 cout <<evenSum;
16 return 0;
17 }

Listing 3.3: Code snippet in L A TEX and this is a C/C++ code example
3.6 Example of in-text citation style
3.6.1 Example of the equations and illustrations placement and reference in
the text
Make sure whenever you refer to the equations, tables, figures, algorithms, and listings for the
first time, they also appear (placed) somewhere on the same page or in the following page(s).
Always make sure to refer to the equations, tables and figures used in the report. Do not
leave them without an in-text citation. You can refer to equations, tables and figures more
them once.
3.6.2 Example of the equations and illustrations style
Write Eq. with an uppercase “Eq“ for an equation before using an equation number with
(\eqref{.}). Use “Table” to refer to a table, “Figure” to refer to a figure, “Algorithm” to refer
to an algorithm and “Listing” to refer to listings (code snippets). Note that, we do not use
CHAPTER 3. METHODOLOGY 13
the articles “a,” “an,” and “the” before the words Eq., Figure, Table, and Listing, but you
may use an article for referring the words figure, table, etc. in general.
For example, the sentence “A report structure is shown in the Table 3.1” should be written
as “A report structure is shown in Table 3.1.”

3.6.3 Tools for In-text Referencing
You will have noticed that there are linked references within the text to specific items in
this document (e.g., equations, figures, tables, chapters, sections, etc.). This is enabled by
a combination of\label{}and\ref{}commands. The former is typically “attached” to
an object/section to be labeled, for instance:\section{My Section}\label{sec:my}. This label,
sec:my, can then be used to create an in-text reference (with link) to the referenced object:
\ref{sec:my}.
The in-text references to the preceding equation were written as:Eq.~\eqref{eq:eq_example
}. Here, the author needed to explicitly write theEq.text, include a tilde,~, to ensure that
the text is not separated from the number at a line break, and usedeqrefto automate
placement of parentheses around the number. Alternatively, we could use the cleverref system
to reference this item with\Cref{eq:eq_example}, yielding: Equation (3.1). This makes
the textual part (Equation) automatic along with spacing and other formatting. The capital
Cin that command specifies capitalisation of the word, whereas lowercase for a figure item
would,\cref{fig:chart_a}would yield a lowercase abbreviated form: fig. 3.1.

3.7 Summary
Write a summary of this chapter.

Note: In the case of software engineering project a Chapter “ Testing and Validation ”
should precede the “Results” chapter. See Section 3.1.1 for report organization of such project.

Chapter 4
Results
The results chapter tells a reader about your findings based on the methodology you have
used to solve the investigated problem. For example:

If your project aims to develop a software/web application, the results may be the
developed software/system/performance of the system, etc., obtained using a relevant
methodological approach in software engineering.
If your project aims to implement an algorithm for its analysis, the results may be the
performance of the algorithm obtained using a relevant experiment design.
If your project aims to solve some problems/research questions over a collected dataset,
the results may be the findings obtained using the applied tools/algorithms/etc.
Arrange your results and findings in a logical sequence.

4.1 A section
...
14
CHAPTER 4. RESULTS 15
4.2 Example of a Table in L A TEX
Table 4.1 is an example of a table created using the package L A TEX“booktabs.” do check
the link: wikibooks.org/wiki/LaTeX/Tables for more details. A table should be clean and
readable. Unnecessary horizontal lines and vertical lines in tables make them unreadable and
messy. The example in Table 4.1 uses a minimum number of liens (only necessary ones).
Make sure that the top rule and bottom rule (top and bottom horizontal lines) of a table are
present.

Table 4.1: Example of a table in L A TEX
Bike
Type Color Price (£)
Electric black 700
Hybrid blue 500
Road blue 300
Mountain red 300
Folding black 500
4.3 Example of captions style
The caption of a Figure (artwork) goes below the artwork (Figure/Graphics/illus-
tration). See example artwork in Figure 3.1.
The caption of a Table goes above the table. See the example in Table 4.1.
The caption of an Algorithm goes above the algorithm. See the example in Algo-
rithm 1.
The caption of a Listing goes below the Listing (Code snippet). See example listing
in Listing 3.1.
4.4 Summary
Write a summary of this chapter.

Chapter 5
Discussion and Analysis
Depending on the type of project you are doing, this chapter can be merged with “Results”
Chapter as “ Results and Discussion” as suggested by your supervisor.
In the case of software development and the standalone applications, describe the signifi-
cance of the obtained results/performance of the system.

5.1 A section
Discussion and analysis chapter evaluates and analyses the results. It interprets the obtained
results.

5.2 Significance of the findings
In this chapter, you should also try to discuss the significance of the results and key findings,
in order to enhance the reader’s understanding of the investigated problem

5.3 Limitations
Discuss the key limitations and potential implications or improvements of the findings.

5.4 Summary
Write a summary of this chapter.

16
Chapter 6
Conclusions and Future Work
6.1 Conclusions
Typically a conclusions chapter first summarizes the investigated problem and its aims and
objectives. It summaries the critical/significant/major findings/results about the aims and
objectives that have been obtained by applying the key methods/implementations/experiment
set-ups. A conclusions chapter draws a picture/outline of your project’s central and the most
signification contributions and achievements.
A good conclusions summary could be approximately 300–500 words long, but this is just
a recommendation.
A conclusions chapter followed by an abstract is the last things you write in your project
report.

6.2 Future work
This section should refer to Chapter 4 where the author has reflected their criticality about
their own solution. The future work is then sensibly proposed in this section.
Guidance on writing future work: While working on a project, you gain experience and
learn the potential of your project and its future works. Discuss the future work of the project
in technical terms. This has to be based on what has not been yet achieved in comparison
to what you had initially planned and what you have learned from the project. Describe to a
reader what future work(s) can be started from the things you have completed. This includes
identifying what has not been achieved and what could be achieved.
A good future work summary could be approximately 300–500 words long, but this is just
a recommendation.

17
Chapter 7
Reflection
Write a short paragraph on the substantial learning experience. This can include your decision-
making approach in problem-solving.
Some hints: You obviously learned how to use different programming languages, write
reports in L A TEXand use other technical tools. In this section, we are more interested in what
you thought about the experience. Take some time to think and reflect on your individual
project as an experience, rather than just a list of technical skills and knowledge. You may
describe things you have learned from the research approach and strategy, the process of
identifying and solving a problem, the process research inquiry, and the understanding of the
impact of the project on your learning experience and future work.
Also think in terms of:

what knowledge and skills you have developed
what challenges you faced, but was not able to overcome
what you could do this project differently if the same or similar problem would come
rationalize the divisions from your initial planed aims and objectives.
A good reflective summary could be approximately 300–500 words long, but this is just a
recommendation.

Note: The next chapter is “ References ,” which will be automatically generated if you
are using BibTeX referencing method. This template uses BibTeX referencing. Also, note
that there is difference between “References” and “Bibliography.” The list of “References”
strictly only contain the list of articles, paper, and content you have cited (i.e., refereed) in
the report. Whereas Bibliography is a list that contains the list of articles, paper, and content
you have cited in the report plus the list of articles, paper, and content you have read in order
to gain knowledge from. We recommend to use only the list of “References.”

18
References
Kottwitz, S. (2021), LaTeX beginner’s guide : create visually appealing texts, articles, and
books for business and science using LaTeX , Packt. ISBN: 9781801072588.

Lamport, L. (1994), LATEX: a document preparation system: user’s guide and reference
manual , Addison-wesley.

University of Reading (2023 a ), ‘Avoiding unintentional plagiarism: Guidance on citing refer-
ences for students at the university of reading: Styles of referencing’. (Accessed: 6 June
2023).
URL: https://libguides.reading.ac.uk/citing-references/avoidingplagiarism

University of Reading (2023 b ), ‘Styles of referencing: Guidance on citing references for stu-
dents at the university of reading’. (Accessed: 6 June 2023).
URL: https://libguides.reading.ac.uk/citing-references/referencingstyles

19
Appendix A

A An Appendix Chapter (Optional)
Some lengthy tables, codes, raw data, length proofs, etc. which are very important but not
essential part of the project report goes into an Appendix. An appendix is something a reader
would consult if he/she needs extra information and a more comprehensive understating of
the report. Also, note that you should use one appendix for one idea.
An appendix is optional. If you feel you do not need to include an appendix in your report,
avoid including it. Sometime including irrelevant and unnecessary materials in the Appendices
may unreasonably increase the total number of pages in your report and distract the reader.

20
Appendix B

B An Appendix Chapter (Optional)
...
21